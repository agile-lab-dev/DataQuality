package it.agilelab.bigdata.DataQuality.utils.io

import java.io.{File, InputStreamReader}

import com.typesafe.config.{Config, ConfigFactory, ConfigObject}
import it.agilelab.bigdata.DataQuality.configs.{GenStructColumn, StructColumn, StructFixedColumn}
import it.agilelab.bigdata.DataQuality.exceptions.IllegalParameterException
import it.agilelab.bigdata.DataQuality.sources.{HdfsFile, OutputFile}
import it.agilelab.bigdata.DataQuality.utils.{DQSettings, Logging, PathUtils}
import org.apache.avro.Schema
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Row, SQLContext}
import org.joda.time.DateTime

import scala.collection.JavaConversions._
import scala.util.Try

/**
  * Created by Gianvito Siciliano on 13/12/16.
  *
  * HDFS reading manager
  */
object HdfsReader extends Logging {

  /**
    * Function-aggregator to read from HDFS
    * @param inputConf input configuration
    * @param refDate date to save on. Used for date replacing
    * @param fs file system
    * @param sqlContext sql context
    * @param settings dataquality configuration
    * @return sequency of dataframes
    */
  def load(inputConf: HdfsFile, refDate: DateTime)(
      implicit fs: FileSystem,
      sqlContext: SQLContext,
      settings: DQSettings): Seq[DataFrame] = {

    log.warn(refDate)
    // replaces {{yyyyMMdd}} in the source path
    val finalPath = PathUtils.replaceDateInPath(inputConf.path, refDate)

    inputConf.fileType.toUpperCase match {
      case "CSV"     => loadCsv(inputConf, finalPath)
      case "PARQUET" => loadParquet(inputConf, finalPath)
      case "FIXED"   => loadWithSchema(inputConf, finalPath)
      case "AVRO"    => loadAvro(inputConf, finalPath)
      case _         => throw new Exception(inputConf.toString)
    }
  }

  /**
    * Loads a DQ output file as a DataFrame
    * WARNING legacy code. Will be refactored or deleted in future builds
    * @param inputConf input configuration (subtype = output)
    * @param fs file system
    * @param sqlContext sql context
    * @return sequence of dataframes
    */
  def loadOutput(inputConf: OutputFile)(
      implicit fs: FileSystem,
      sqlContext: SQLContext): Seq[(String, DataFrame)] = {
    import sqlContext.implicits._

    log.info(s"Starting load ${inputConf.fileType.toUpperCase}")

    if (!fs.exists(new Path(inputConf.path))) {
      log.warn("fixed input file: " + inputConf.path + " not found!")
      Nil
    } else {
      log.warn("loading fixed input file: " + inputConf.id)

      val fileMetrics = sqlContext.sparkContext.textFile(inputConf.path + "/")
      val columnMetrics = sqlContext.sparkContext.textFile(inputConf.path + "/")

      val columnData =
        sqlContext.read
          .format("com.databricks.spark.csv")
          .option("header", inputConf.header.toString)
          .option("delimiter", inputConf.separator.get)
          .load(inputConf.path + "/COLUMNAR-METRICS*")

      val fileData =
        sqlContext.read
          .format("com.databricks.spark.csv")
          .option("header", inputConf.header.toString)
          .option("delimiter", inputConf.separator.get)
          .load(inputConf.path + "/FIXED-FILE-METRICS*")

      val df = columnData.drop(columnData.col("columnName")).unionAll(fileData)
      val casted =
        df.withColumn("result", df("result").cast(DataTypes.DoubleType))

      val ids =
        casted.select("id").distinct.collect.flatMap(_.toSeq).map(_.toString)
      ids.map(id => id -> casted.where($"id" <=> id))
    }

  }

  /**
    * Loads fixed length file (separation based on length of the fields) from HDFS
    * @param inputConf input configuration (subtype = FIXED)
    * @param filePath path to the file
    * @param fs file system
    * @param sqlContext sql context
    * @return sequence of dataframes
    */
  private def loadWithSchema(inputConf: HdfsFile, filePath: String)(
      implicit fs: FileSystem,
      sqlContext: SQLContext): Seq[DataFrame] = {
    log.info(
      s"Starting load ${inputConf.fileType.toUpperCase} file -> ${filePath}")

    val result: Option[DataFrame] = if (!fs.exists(new Path(filePath))) {
      log.warn("fixed input file: " + filePath + " not found!")
      None
    } else {
      log.warn("loading fixed input file: " + inputConf.id)

      val fieldSeq: List[GenStructColumn] = inputConf.schema.get match {
        case xs: List[_] =>
          xs.filter(
              _ match { case _: GenStructColumn => true; case _ => false })
            .asInstanceOf[List[GenStructColumn]]
        case s: String => tryToLoadSchema(s)
        case e         => throw IllegalParameterException(e.toString)
      }

      val ff: RDD[Row] = sqlContext.sparkContext.textFile(filePath).map { x =>
        getRow(x, fieldSeq)
      }
      val schema = StructType(
        fieldSeq.map(x => StructField(x.name, StringType, nullable = true)))

      Option(sqlContext.createDataFrame(ff, schema))
    }

    result.map(Seq(_)).getOrElse(Nil)
  }

  /**
    * Gets spark row from string and fields. Used in fixed file parsing
    * @param x String to parse
    * @param fields Fields to parse
    * @return Row
    */
  private def getRow(x: String, fields: List[GenStructColumn]) = {
    val columnArray = new Array[String](fields.size)
    var pos = 0
    fields.head.getType match {
      case "StructFixedColumn" =>
        val ll: List[StructFixedColumn] =
          fields.map(_.asInstanceOf[StructFixedColumn])
        ll.zipWithIndex.foreach { field =>
          columnArray(field._2) =
            Try(x.substring(pos, pos + field._1.length).trim).getOrElse(null)
          pos += field._1.length
        }
      case _ => IllegalParameterException(fields.head.toString)
    }
    Row.fromSeq(columnArray)
  }

  /**
    * Loads avro file from HDFS
    * @param inputConf input configuration
    * @param filePath path to file
    * @param fs file system
    * @param sqlContext sql context
    * @param settings dataquality configuration
    * @return sequence of dataframes
    */
  private def loadAvro(inputConf: HdfsFile, filePath: String)(
      implicit fs: FileSystem,
      sqlContext: SQLContext,
      settings: DQSettings): Seq[DataFrame] = {
    log.info(
      s"Starting load ${inputConf.fileType.toUpperCase} file -> ${filePath}")

    val result: Option[DataFrame] =
      if (!fs.exists(new Path(filePath))) {
        log.warn("avro input file: " + inputConf.id + " not found!")
        None
      } else {
        log.warn("loading avro input file: " + inputConf.id)

        // It's possible to provide a scheme, so the following code splits the workflow
        val schema = Try {
          inputConf.schema.get match {
            case str: String =>
              Try {
                new Schema.Parser().parse(new File(str))
              }.getOrElse(
                new Schema.Parser().parse(fs.open(new Path(str)))
              )
            case e => IllegalParameterException(e.toString)
          }
        }.toOption

        val res = schema match {
          case Some(sc) =>
            sqlContext.read
              .format("com.databricks.spark.avro")
              .option("avroSchema", sc.toString)
              .load(filePath)
          case None =>
            if (inputConf.schema.isDefined)
              log.warn("Failed to load the schema from file")
            sqlContext.read
              .format("com.databricks.spark.avro")
              .load(filePath)
        }

        if (settings.repartition)
          Some(res.repartition(sqlContext.sparkContext.defaultParallelism))
        else
          Some(res)
      }
    result.map(Seq(_)).getOrElse(Nil)
  }

  /**
    * Loads Parquet file from HDFS
    * @param inputConf input configuration
    * @param filePath hdfs path to file
    * @param fs file system
    * @param sqlContext sql context
    * @return sequence of dataframes
    */
  private def loadParquet(inputConf: HdfsFile, filePath: String)(
      implicit fs: FileSystem,
      sqlContext: SQLContext): Seq[DataFrame] = {
    log.info(
      s"Starting load ${inputConf.fileType.toUpperCase} file -> ${filePath}")

    val result: Option[DataFrame] =
      if (!fs.exists(new Path(filePath))) {
        log.warn("parquet input file: " + inputConf.id + " not found!")
        None
      } else {
        log.warn("loading parquet input file: " + inputConf.id)

        val res = sqlContext.read
          .parquet(filePath)
        Some(res)
      }
    result.map(Seq(_)).getOrElse(Nil)
  }

  /**
    * Reads CSV file from HDFS
    * @param inputConf input configuration
    * @param filePath hdfs path to file
    * @param fs file system
    * @param sqlContext sql context
    * @param settings dataquality configuration
    * @return sequence of dataframes
    */
  private def loadCsv(inputConf: HdfsFile, filePath: String)(
      implicit fs: FileSystem,
      sqlContext: SQLContext,
      settings: DQSettings): Seq[DataFrame] = {
    log.info(
      s"Starting load ${inputConf.fileType.toUpperCase} file -> ${filePath}")

    val schema: Option[List[StructField]] = Try {
      inputConf.schema.get match {
        case list: List[_] =>
          list.map {
            case col: StructColumn =>
              val typeName: DataType = mapToDataType(col.tipo)
              StructField(col.name, typeName, true)
            case x => throw IllegalParameterException(x.toString)
          }
        case str: String =>
          val colList: List[GenStructColumn] = tryToLoadSchema(str)
          colList.map(col => {
            val typeName: DataType = mapToDataType(col.tipo)
            StructField(col.name, typeName, true)
          })
        case e => throw IllegalParameterException(e.toString)
      }
    }.toOption

    log.info("schema " + schema)

    val resultReader = sqlContext.read
      .format("com.databricks.spark.csv")
      .option("header", inputConf.header.toString)
      .option("delimiter", inputConf.separator.getOrElse(","))

    val result = schema match {
      case Some(sc) => resultReader.schema(StructType(sc)).load(filePath)
      case None     => resultReader.load(filePath)
    }

//      log.info("result count "+result.count())
    if (settings.repartition)
      return Seq(result.repartition(sqlContext.sparkContext.defaultParallelism))

    Seq(result)
  }

  /**
    * Remove quotes from string
    * WARNING Not used anywhere. Probably will be removed
    * @param value string to operate with
    * @param quote quote char
    * @return "clean" string
    */
  private def removeQuotes(value: String, quote: String = "\"") = {

    var result = value

    while (result != null && result.startsWith(quote)) result =
      result.substring(1)

    while (result != null && result.endsWith(quote)) if (result == quote)
      result = ""
    else
      result = result.substring(0, result.length - 1)

    result
  }

  /**
    * Maps string to spark DataType
    * Used in schema parsing
    * @param colType string to parse
    * @return parsed datatype
    */
  private def mapToDataType(colType: String): DataType = {
    colType.toUpperCase match {
      case "STRING"  => StringType
      case "DOUBLE"  => DoubleType
      case "INTEGER" => IntegerType
      case "DATE"    => DateType
      case e =>
        log.warn("Failed to load schema")
        throw IllegalParameterException(e)
    }
  }

  /**
    * Tries to load schema from file, otherwise throw an exeption
    * @param filePath path to schema
    * @param fs file system
    * @return list of column object
    */
  private def tryToLoadSchema(filePath: String)(
      implicit fs: FileSystem): List[GenStructColumn] = {
    if (!fs.exists(new Path(filePath))) {
      log.warn("Schema does not exists")
      throw IllegalParameterException(filePath)
    } else {
      val configObj: Config = Try {
        ConfigFactory.parseFile(new File(filePath)).resolve()
      }.getOrElse(Try {
        val inputStream = fs.open(new Path(filePath))
        val reader = new InputStreamReader(inputStream)

        ConfigFactory.parseReader(reader)
      }.getOrElse(throw IllegalParameterException(filePath)))

      val structColumns: List[ConfigObject] =
        configObj.getObjectList("Schema").toList
      structColumns.map(col => {
        val conf = col.toConfig
        val name = conf.getString("name")
        val typeConf = conf.getString("type")
        Try {
          conf.getInt("length")
        }.toOption match {
          // there are 2 types of column, one for FIXED files and for regular CSV.
          // CSV schema can be in both forms, but length field will not affect the loading
          // you should use metrics and checks to operate with values length
          case Some(l) => StructFixedColumn(name, typeConf, l)
          case None    => StructColumn(name, typeConf)
        }
      })
    }
  }

}
