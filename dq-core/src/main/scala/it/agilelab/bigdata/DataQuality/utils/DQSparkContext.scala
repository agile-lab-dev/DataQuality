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
      .setAppName(s"${settings.appName} Data Quality")
      .set("spark.serializer", serializerClassName)
      .set("spark.kryoserializer.buffer.max", "128")
      .set("spark.sql.parquet.compression.codec", "snappy")

    if (settings.local) conf.setMaster("local[*]")
    if (settings.s3Bucket.isDefined) conf.set("spark.sql.warehouse.dir", settings.s3Bucket + "/data_quality_output/spark/warehouse")
    if (settings.hbaseHost.isDefined) conf.set("spark.hbase.host", settings.hbaseHost.get)
    f(conf)
  }

  protected def makeSparkContext(settings: DQSettings): SparkContext =
    withSparkConf(settings) { conf =>
      new SparkContext(conf)
    }

  protected def makeSqlContext(sc: SparkContext): SQLContext = {
    new SQLContext(sc)
  }

}
