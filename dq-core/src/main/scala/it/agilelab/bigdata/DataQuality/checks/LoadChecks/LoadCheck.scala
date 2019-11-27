package it.agilelab.bigdata.DataQuality.checks.LoadChecks
import it.agilelab.bigdata.DataQuality.checks.{CheckStatusEnum, LoadCheckResult}
import org.apache.hadoop.fs.FileSystem
import it.agilelab.bigdata.DataQuality.checks.LoadChecks.ExeEnum.LCType
import it.agilelab.bigdata.DataQuality.sources.{HdfsFile, SourceConfig, SourceTypes}
import it.agilelab.bigdata.DataQuality.utils.{DQSettings, Logging}
import org.apache.spark.sql.{DataFrame, SQLContext}

import scala.util.Try

/**
  * Created by Egor Makhov on 07/05/19.
  */
object ExeEnum extends Enumeration {
  type LCType = Value
  val pre, post = Value
}

object LoadCheckEnum extends Enumeration {
  // pre
  val existLC     = LCDefinition("EXIST", classOf[ExistLoadCheck])
  val encodingLC  = LCDefinition("ENCODING", classOf[EncodingLoadCheck])
  val fileTypeLC  = LCDefinition("FILE_TYPE", classOf[FileTypeLoadCheck])
  // post
  val columnNumLC = LCDefinition("EXACT_COLUMN_NUM", classOf[ColumnLoadCheck])
  val minColumnNumLC = LCDefinition("MIN_COLUMN_NUM", classOf[MinColumnLoadCheck])

  def getCheckClass(name: String): Class[_ <: LoadCheck with Product with Serializable] =
    convert(super.withName(name)).check
  def names: Set[String]           = values.map(_.toString)
  def contains(s: String): Boolean = names.contains(s)

  protected case class LCDefinition(name: String, check: Class[_ <: LoadCheck with Product with Serializable])
      extends super.Val() {
    override def toString(): String = this.name
  }

  implicit def convert(value: Value): LCDefinition = value.asInstanceOf[LCDefinition]
}

abstract class LoadCheck() {
  def id: String
  def tipo: String
  def source: String
  def option: Any

  val exeType: LCType
  def run(confOpt: Option[SourceConfig] = None, df: Option[DataFrame] = None)(implicit fs: FileSystem,
                                                                              sqlc: SQLContext,
                                                                              settings: DQSettings): LoadCheckResult
}

/**
* Check if the source is present in the defined path
  * @param id Check id
  * @param tipo Check type
  * @param source Source ID
  * @param option Expected result
  */
case class ExistLoadCheck(id: String, tipo: String, source: String, option: Any) extends LoadCheck with Logging {
  override val exeType: LCType = ExeEnum.pre
  override def run(confOpt: Option[SourceConfig] = None,
                   df: Option[DataFrame] = None)(implicit fs: FileSystem, sqlc: SQLContext, settings: DQSettings): LoadCheckResult = {

    val conf = confOpt match {
      case Some(x) if x.getType == SourceTypes.hdfs => x.asInstanceOf[HdfsFile]
      case _                                        => throw new IllegalArgumentException("Encoding load check can be used only on HDFS files.")
    }

    val expected: Boolean = option.toString.toBoolean

    val (status, msg) = (fs.exists(new org.apache.hadoop.fs.Path(conf.path)), expected) match {
      case (true, true) => (CheckStatusEnum.Success, "Source is present in the file system")
      case (false, false) => (CheckStatusEnum.Success, "Source is not present in the file system")
      case (false, true) => (CheckStatusEnum.Failure, s"No files have been found at path: ${conf.path}")
      case (true, false) => (CheckStatusEnum.Failure, s"Some files have been found at path: ${conf.path}")
    }

    LoadCheckResult(id, source, tipo, option.toString, settings.refDateString, status, msg)
  }
}

/**
* Checks if the source is loadable with the following encoding
  * @param id Check id
  * @param tipo Check type
  * @param source Source ID
  * @param option Encoding name (please, use encoding names defined in Spark)
  */
case class EncodingLoadCheck(id: String, tipo: String, source: String, option: Any) extends LoadCheck with Logging {
  override val exeType: LCType = ExeEnum.pre

  override def run(confOpt: Option[SourceConfig] = None,
                   df: Option[DataFrame] = None)(implicit fs: FileSystem, sqlc: SQLContext, settings: DQSettings): LoadCheckResult = {

    val conf: HdfsFile = confOpt match {
      case Some(x) if x.getType == SourceTypes.hdfs => x.asInstanceOf[HdfsFile]
      case _                                        => throw new IllegalArgumentException("Encoding load check can be used only on HDFS files.")
    }

    val dfOpt: Option[DataFrame] = conf.fileType.toLowerCase match {
      case "avro" =>
        Try(
          sqlc.read
            .format("com.databricks.spark.avro")
            .option("encoding", option.toString)
            .load(conf.path)).toOption
      case "csv" =>
        Try(
          sqlc.read
            .format("com.databricks.spark.csv")
            .option("header", conf.header.toString)
            .option("delimiter", conf.delimiter.getOrElse(","))
            .option("quote", conf.quote.getOrElse("\""))
            .option("escape", conf.escape.getOrElse("\\"))
            .option("encoding", option.toString)
            .load(conf.path)
        ).toOption
      case _ => throw new IllegalArgumentException(s"Unknown source type: $option.")
    }

    val (status, msg) = dfOpt match {
      case Some(_) => (CheckStatusEnum.Success, "")
      case None    => (CheckStatusEnum.Failure, s"Source can't be loaded with encoding: ${option.toString}")
    }

    LoadCheckResult(id, source, tipo, option.toString, settings.refDateString, status, msg)
  }
}

/**
* Checks if the source is loadable in the desired format
  * @param id Check id
  * @param tipo Check type
  * @param source Source ID
  * @param option File format (csv, avro)
  */
case class FileTypeLoadCheck(id: String, tipo: String, source: String, option: Any) extends LoadCheck with Logging {
  override val exeType: LCType = ExeEnum.pre
  override def run(confOpt: Option[SourceConfig] = None,
                   df: Option[DataFrame] = None)(implicit fs: FileSystem, sqlc: SQLContext, settings: DQSettings): LoadCheckResult = {

    val conf: HdfsFile = confOpt match {
      case Some(x) if x.getType == SourceTypes.hdfs => x.asInstanceOf[HdfsFile]
      case _                                        => throw new IllegalArgumentException("Encoding load check can be used only on HDFS files.")
    }

    val dfOpt: Option[DataFrame] = option.toString.toLowerCase match {
      case "avro" =>
        Try(
          sqlc.read
            .format("com.databricks.spark.avro")
            .load(conf.path)).toOption
      case "csv" =>
        Try(
          sqlc.read
          .format("com.databricks.spark.csv")
          .option("header", conf.header.toString)
          .option("delimiter", conf.delimiter.getOrElse(","))
          .option("quote", conf.quote.getOrElse("\""))
          .option("escape", conf.escape.getOrElse("\\"))
          .load(conf.path)
        ).toOption
      case _ => throw new IllegalArgumentException(s"Unknown source type: $option.")
    }

    val (status, msg) = dfOpt match {
      case Some(_) => (CheckStatusEnum.Success, "")
      case None    => (CheckStatusEnum.Failure, s"Source can't be loaded as ${option.toString.toLowerCase}")
    }

    LoadCheckResult(id, source, tipo, option.toString, settings.refDateString, status, msg)
  }
}

/**
* Checks if #columns of the source is the same as desired number
  * @param id Check id
  * @param tipo Check type
  * @param source Source ID
  * @param option Num of columns
  */
case class ColumnLoadCheck(id: String, tipo: String, source: String, option: Any) extends LoadCheck with Logging {
  override val exeType: LCType = ExeEnum.post
  override def run(confOpt: Option[SourceConfig] = None,
                   df: Option[DataFrame] = None)(implicit fs: FileSystem, sqlc: SQLContext, settings: DQSettings): LoadCheckResult = {

    val (status, msg): (CheckStatusEnum.Value, String) = df match {
      case Some(dataframe) =>
        if (dataframe.columns.length == option.toString.toInt) (CheckStatusEnum.Success, "")
        else (CheckStatusEnum.Failure, s"Source #columns {${dataframe.columns.length}} is not equal to $option")
      case None => (CheckStatusEnum.Error, "DataFrame hasn't been found")
    }
    LoadCheckResult(id, source, tipo, option.toString, settings.refDateString, status, msg)
  }
}


/**
  * Checks if #columns of the source is more or equal to the desired number
  * @param id Check id
  * @param tipo Check type
  * @param source Source ID
  * @param option Min num of columns
  */
case class MinColumnLoadCheck(id: String, tipo: String, source: String, option: Any) extends LoadCheck with Logging {
  override val exeType: LCType = ExeEnum.post
  override def run(confOpt: Option[SourceConfig] = None,
                   df: Option[DataFrame] = None)(implicit fs: FileSystem, sqlc: SQLContext, settings: DQSettings): LoadCheckResult = {

    val (status, msg): (CheckStatusEnum.Value, String) = df match {
      case Some(dataframe) =>
        if (dataframe.columns.length >= option.toString.toInt) (CheckStatusEnum.Success, s"${dataframe.columns.length} >= ${option.toString.toInt}")
        else (CheckStatusEnum.Failure, s"Source #columns {${dataframe.columns.length}} is less than $option")
      case None => (CheckStatusEnum.Error, "DataFrame hasn't been found")
    }
    LoadCheckResult(id, source, tipo, option.toString, settings.refDateString, status, msg)
  }
}

