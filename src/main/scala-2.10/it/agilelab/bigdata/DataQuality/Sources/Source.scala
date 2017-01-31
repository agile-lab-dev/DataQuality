package it.agilelab.bigdata.DataQuality.sources

import it.agilelab.bigdata.DataQuality.configs.GenStructField
import org.apache.spark.sql.DataFrame

/**
  * Created by Gianvito Siciliano on 03/01/17.
  */
trait SourceConfig{
  def getType: String //TODO enum
}

case class HdfsFile(
                     id:String,
                     path:String,
                     fileType:String,
                     separator:Option[String],
                     header: Boolean,
                     date: String,
                     dependencies: List[String] = List.empty[String],
                     schema: Option[List[GenStructField]] = None
                   )
  extends SourceConfig  {
    override def getType: String = "HDFS"
  }



case class Source(
                   id: String,
                   df: DataFrame
                 )