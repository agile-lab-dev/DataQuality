package it.agilelab.bigdata.DataQuality.targets

/**
  * Created by Gianvito Siciliano on 02/01/17.
  */
trait TargetConfig{
  def getType: String

}

case class HdfsTargetConfig(
                     fileName: String,
                     fileFormat:String,
                     path:String,
                     ruleName:String,
                     delimiter:Option[String],
                     date: String,
                     dependencies: List[String] = List.empty[String],
                     savemode: Option[String],
                     partitions: Int = 1
                   )
  extends TargetConfig  {
    override def getType: String = "HDFS"
  }

