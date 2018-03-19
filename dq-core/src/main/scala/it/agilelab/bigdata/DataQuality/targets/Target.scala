package it.agilelab.bigdata.DataQuality.targets

/**
  * Created by Gianvito Siciliano on 02/01/17.
  */
trait TargetConfig {
  def getType: String
}

case class SystemTargetConfig(
    id: String,
    checkList: Seq[String],
    mailList: Seq[String],
    outputConfig: TargetConfig
) extends TargetConfig {
  override def getType: String = "SYSTEM"
}

/**
  * Representation of file to save
  */
case class HdfsTargetConfig(
    fileName: String,
    fileFormat: String,
    path: String,
    delimiter: Option[String] = None,
    date: Option[String] = None,
    savemode: Option[String] = None,
    quoted: Boolean = false
) extends TargetConfig {
  override def getType: String = "HDFS"
}
