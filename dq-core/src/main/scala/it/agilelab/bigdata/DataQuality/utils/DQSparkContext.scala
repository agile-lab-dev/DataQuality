package it.agilelab.bigdata.DataQuality.utils

import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by Paolo on 20/01/2017.
  */
trait DQSparkContext {

  protected def serializerClassName =
    "org.apache.spark.serializer.KryoSerializer"

  protected def withSparkConf(settings: DQSettings)(
      f: SparkConf => SparkContext): SparkContext = {
    val conf = new SparkConf()
      .setAppName(settings.appName)
      .set("spark.serializer", serializerClassName)
      .set("spark.kryoserializer.buffer.max", "128")
      .set("spark.sql.parquet.compression.codec", "snappy")
    if (settings.local) conf.setMaster("local[*]")
    if (settings.hbaseHost.nonEmpty) conf.set("spark.hbase.host", settings.hbaseHost)
    f(conf)
  }

  protected def makeSparkContext(settings: DQSettings): SparkContext =
    withSparkConf(settings) { conf =>
      new SparkContext(conf)
    }

  protected def makeSqlContext(sc: SparkContext) = {
    new SQLContext(sc)
  }

}
