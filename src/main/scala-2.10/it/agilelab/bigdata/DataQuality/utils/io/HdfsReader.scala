package it.agilelab.bigdata.DataQuality.utils.io

import it.agilelab.bigdata.DataQuality.configs.{GenStructField, StructFixedField}
import it.agilelab.bigdata.DataQuality.sources.HdfsFile
import it.agilelab.bigdata.DataQuality.utils.{Logging, PathUtils}
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row, SQLContext}
import org.joda.time.DateTime

import scala.util.Try

/**
  * Created by Gianvito Siciliano on 13/12/16.
  */
object HdfsReader extends Logging {

  def load(inputConf: HdfsFile, refDate: DateTime)(implicit fs: FileSystem, sqlContext: SQLContext): Option[DataFrame]  = {

    val finalPath = PathUtils.replaceDateInPath(inputConf.path, refDate)

    inputConf.fileType.toUpperCase match {
      case "CSV"      => loadCsv(inputConf, finalPath)
      case "PARQUET"  => loadParquet(inputConf, finalPath)
      case "FIXED"    => loadWithSchema(inputConf, finalPath)
      case _          => throw new Exception(inputConf.toString)
    }
  }

  private def loadWithSchema(inputConf: HdfsFile, filePath: String)(implicit fs: FileSystem, sqlContext: SQLContext) : Option[DataFrame] = {
    log.info(s"Starting load ${inputConf.fileType.toUpperCase} file -> ${filePath}")

    val result: Option[DataFrame] = if (!fs.exists(new Path(filePath))) {
      log.warn("fixed input file: " + filePath + " not found!")
      None
    }
    else {
      log.warn("loading fixed input file: " + inputConf.id)

      val fieldSeq: List[GenStructField] = inputConf.schema.get
      val ff: RDD[Row] = sqlContext.sparkContext.textFile(filePath).map { x => getRow(x, fieldSeq) }

      val schema = StructType(fieldSeq.map(x => StructField(x.name, StringType, nullable = true)))

      Option(sqlContext.createDataFrame(ff, schema))
    }


    result
  }

  private def getRow(x : String, fields: List[GenStructField]) = {
    val columnArray = new Array[String](fields.size)
    var pos = 0
    fields.head.getType match {
      case "StructFixedField" => {
        val ll: List[StructFixedField] = fields.map(_.asInstanceOf[StructFixedField])
        ll.zipWithIndex.foreach{ field =>
        columnArray(field._2) = Try(x.substring(pos, pos+field._1.length).trim).getOrElse(null)
          pos+=field._1.length
        }
      }
      case "StructField"      => ???
      case _ => ???
    }
    Row.fromSeq(columnArray)
  }

  private def loadParquet(inputConf: HdfsFile, filePath: String)(implicit fs: FileSystem, sqlContext: SQLContext) : Option[DataFrame] = {
    log.info(s"Starting load ${inputConf.fileType.toUpperCase} file -> ${filePath}")

    val result: Option[DataFrame] =
      if (!fs.exists(new Path(filePath))) {
        log.warn("parquet input file: " + inputConf.id + " not found!")
        None
      }
      else {
        log.warn("loading parquet input file: " + inputConf.id)

        val res = sqlContext
          .read
          .parquet(filePath)
        Some(res)
      }
    result
  }

  private def loadCsv(inputConf: HdfsFile, filePath: String)(implicit fs: FileSystem, sqlContext: SQLContext) : Option[DataFrame] = {//(implicit fs: FileSystem, sqlContext: SQLContext): Option[DataFrame] = {
    log.info(s"Starting load ${inputConf.fileType.toUpperCase} file -> ${filePath}")

    val result: Option[DataFrame] = if (!fs.exists(new Path(filePath))) {
      log.warn("csv input file: " + inputConf.id + " not found!")
      None
    }
    else {
      log.warn("loading csv input file: " + inputConf.id)

      val result =
        sqlContext.read
        .format("com.databricks.spark.csv") //TODO csv, parquet...inputConf.fileType
        .option("header", inputConf.header.toString)
        .option("delimiter", inputConf.separator.get)
        //.option("mode", "DROPMALFORMED")
        //.option("charset", settings.abtCharset)
        .load(filePath)

      Some(result)
    }

    result
  }

  private def removeQuotes(value: String, quote: String = "\"") = {

    var result = value

    while (result != null && result.startsWith(quote))
      result = result.substring(1)

    while (result != null && result.endsWith(quote))

      if (result == quote)
        result = ""
      else
        result = result.substring(0, result.length - 1)

    result
  }


}

