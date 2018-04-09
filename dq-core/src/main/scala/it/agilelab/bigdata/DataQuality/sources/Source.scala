package it.agilelab.bigdata.DataQuality.sources

import it.agilelab.bigdata.DataQuality.configs.GenStructColumn
import it.agilelab.bigdata.DataQuality.metrics.CalculatorStatus.Value
import it.agilelab.bigdata.DataQuality.sources.SourceTypes.SourceType
import it.agilelab.bigdata.DataQuality.targets.HdfsTargetConfig
import it.agilelab.bigdata.DataQuality.utils.DQSettings
import org.apache.spark.sql.DataFrame
import scala.reflect._

import scala.reflect.ClassTag

/**
  * Created by Gianvito Siciliano on 03/01/17.
  *
  * Different representation of source formats
  */
object SourceTypes extends Enumeration {
  type SourceType = Value
  val hdfs: SourceType = Value("HDFS")
  val table: SourceType = Value("TABLE")
  val output: SourceType = Value("OUTPUT")
  val virtual: SourceType = Value("VIRTUAL")
  val hive: SourceType = Value("HIVE")
  val hbase: SourceType = Value("HBASE")
}

abstract class SourceConfig {
  def getType: SourceType
  def keyfields: Seq[String]
}

case class HBaseSrcConfig(
                       id: String,
                       table: String,
                       hbaseColumns: Seq[String],
//                       dfColumns: Seq[String],
                       keyfields: Seq[String] = Seq.empty,
                       save: Boolean = false
                       ) extends SourceConfig {
  override def getType: SourceType = SourceTypes.hbase
  def getClassTag = classTag[(String, String)]
}

abstract class VirtualFile(id: String,
                           keyfields: Seq[String],
                           save: Boolean = false)
    extends SourceConfig {
  override def getType: SourceType = SourceTypes.virtual
  def isSave: Boolean = save
  def parentSourceIds: Seq[String]
}

case class VirtualFileSelect(id: String,
                             parentSourceIds: Seq[String],
                             sqlQuery: String,
                             keyfields: Seq[String],
                             save: Boolean = false)
    extends VirtualFile(id, keyfields, save)

case class VirtualFileJoinSql(id: String,
                              parentSourceIds: Seq[String],
                              sqlJoin: String,
                              keyfields: Seq[String],
                              save: Boolean = false)
    extends VirtualFile(id, keyfields, save)

case class VirtualFileJoin(id: String,
                           parentSourceIds: Seq[String],
                           joiningColumns: Seq[String],
                           joinType: String,
                           keyfields: Seq[String],
                           save: Boolean = false)
    extends VirtualFile(id, keyfields, save)

case class HdfsFile(
    id: String,
    path: String,
    fileType: String,
    separator: Option[String],
    header: Boolean,
    date: String,
    dependencies: List[String] = List.empty[String],
    schema: Option[Any] = None,
    keyfields: Seq[String] = Seq.empty
) extends SourceConfig {

  def this(tar: HdfsTargetConfig)(implicit settings: DQSettings) = {
    this(
      tar.fileName,
      tar.path + "/" + tar.fileName + s".${tar.fileFormat}",
      tar.fileFormat,
      tar.delimiter,
      true,
      tar.date.getOrElse(settings.refDateString)
    )
  }

  override def getType: SourceType = SourceTypes.hdfs
}

case class OutputFile(
    id: String,
    path: String,
    fileType: String,
    separator: Option[String],
    header: Boolean,
    date: String,
    dependencies: List[String] = List.empty[String],
    schema: Option[List[GenStructColumn]] = None,
    keyfields: Seq[String] = Seq.empty
) extends SourceConfig {
  override def getType: SourceType = SourceTypes.output
}

case class TableConfig(
    id: String,
    dbId: String,
    table: String,
    username: Option[String],
    password: Option[String],
    keyfields: Seq[String] = Seq.empty
) extends SourceConfig {
  override def getType: SourceType = SourceTypes.table
}

case class HiveTableConfig(
    id: String,
    date: String,
    query: String,
    keyfields: Seq[String] = Seq.empty
) extends SourceConfig {
  override def getType: SourceType = SourceTypes.hive
}

case class Source(
    id: String,
    date: String,
    df: DataFrame,
    keyfields: Seq[String] = Seq.empty
)
