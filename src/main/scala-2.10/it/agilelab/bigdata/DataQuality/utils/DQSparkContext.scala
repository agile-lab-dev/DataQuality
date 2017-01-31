package it.agilelab.bigdata.DataQuality.utils

import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by Paolo on 20/01/2017.
  */


trait DQSparkContext {

  protected def serializerClassName = "org.apache.spark.serializer.KryoSerializer"

  protected def withSparkConf(settings: DQSettings, isLocal: Boolean)(f: SparkConf => SparkContext) = f(
    {
      val c = new SparkConf()
        .setAppName(settings.appName)
        .set("spark.serializer", serializerClassName)
        .set("spark.kryoserializer.buffer.max", "128")
        .set("spark.sql.parquet.compression.codec", "snappy")


      if (isLocal) c.setMaster("local[*]") else c
    }
  )

  protected def makeSparkContext(settings: DQSettings, isLocal:Boolean) = withSparkConf(settings, isLocal) { conf =>
    new SparkContext(conf)
  }

  protected def makeSqlContext(sc: SparkContext) = {
    new SQLContext(sc)
  }


}

