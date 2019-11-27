package it.agilelab.bigdata.DataQuality.targets
import it.agilelab.bigdata.DataQuality.utils.enums
import it.agilelab.bigdata.DataQuality.utils.enums.Targets
import it.agilelab.bigdata.DataQuality.utils.enums.Targets.TargetType

/**
  * Base target trait
  */
trait TargetConfig {
  def getType: TargetType
}

/**
  * System target configuration. Send an email and save a file if some of the checks are failing
  * @param id Target id
  * @param checkList List of check to watch
  * @param mailList List of notification recipients
  * @param outputConfig Output file configuration
  */
case class SystemTargetConfig(
    id: String,
    checkList: Seq[String],
    mailList: Seq[String],
    outputConfig: TargetConfig
) extends TargetConfig {
  override def getType: enums.Targets.Value = Targets.system
}

/**
  * HDFS file target configuration
  * @param fileName Name of the output file
  * @param fileFormat File type (csv, avro)
  * @param path desired path
  * @param delimiter delimiter
  * @param quote quote char
  * @param escape escape char
  * @param date output date
  * @param quoteMode quote mode (refer to spark-csv)
  */
case class HdfsTargetConfig(
    fileName: String,
    fileFormat: String,
    path: String,
    delimiter: Option[String] = None,
    quote: Option[String] = None,
    escape: Option[String] = None,
    date: Option[String] = None,
    quoteMode: Option[String] = None
) extends TargetConfig {
  override def getType: enums.Targets.Value = Targets.hdfs
}
