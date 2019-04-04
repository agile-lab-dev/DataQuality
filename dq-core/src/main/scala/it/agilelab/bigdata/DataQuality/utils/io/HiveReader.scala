package it.agilelab.bigdata.DataQuality.utils.io

import it.agilelab.bigdata.DataQuality.sources.HiveTableConfig
import it.agilelab.bigdata.DataQuality.utils.Logging
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.hive.HiveContext

import scala.util.Try

/**
  * Created by Egor Makhov on 16/06/2017.
  *
  * Hive manager
  */
object HiveReader extends Logging {

  /**
    * Runs datasource query in Hive
    * @param inputConf Hive datasource configuration
    * @param hiveContext Application Hive context
    * @return result of the query
    */
  def loadHiveTable(inputConf: HiveTableConfig)(
      implicit hiveContext: HiveContext): Seq[DataFrame] = {

    // You can specify a template for queries here. Currently it's just an input query as it is
    val full_query = inputConf.query
    Try {
      Seq(hiveContext.sql(full_query))
    }.getOrElse({
      log.warn("Failed to load HIVE table")
      Seq.empty
    })
  }
}
