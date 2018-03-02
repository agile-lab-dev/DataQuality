package it.agilelab.bigdata.DataQuality.sources

import it.agilelab.bigdata.DataQuality.configs.GenStructColumn
import it.agilelab.bigdata.DataQuality.targets.HdfsTargetConfig
import it.agilelab.bigdata.DataQuality.utils.DQSettings
import org.apache.spark.sql.DataFrame

/**
  * Created by Gianvito Siciliano on 03/01/17.
  *
  * Different representation of source formats
  */
abstract class SourceConfig {
  def getType: String //TODO enum
  def keyfields: Seq[String]
}

abstract class VirtualFile(id: String,
                           keyfields: Seq[String],
                           save: Boolean = false)
    extends SourceConfig {
  override def getType: String = "VIRTUAL"
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

  override def getType: String = "HDFS"
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
  override def getType: String = "OUTPUT"
}

case class TableConfig(
    id: String,
    dbId: String,
    table: String,
    username: Option[String],
    password: Option[String],
    keyfields: Seq[String] = Seq.empty
) extends SourceConfig {
  override def getType: String = "TABLE"
}

case class HiveTableConfig(
    id: String,
    date: String,
    query: String,
    keyfields: Seq[String] = Seq.empty
) extends SourceConfig {
  override def getType: String = "HIVE"
}

case class Source(
    id: String,
    date: String,
    df: DataFrame,
    keyfields: Seq[String] = Seq.empty
)
