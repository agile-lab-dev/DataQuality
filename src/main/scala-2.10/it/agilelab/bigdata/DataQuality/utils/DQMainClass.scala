package it.agilelab.bigdata.DataQuality.utils

import org.apache.hadoop.fs.FileSystem
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext

/**
  * Created by Paolo on 20/01/2017.
  */


trait DQMainClass { this: DQSparkContext with Logging  =>

  private def initLogger(): Unit = {

    Logger.getLogger("org").setLevel(Level.WARN)
    Logger.getLogger("org.apache.spark.scheduler.TaskSetManager").setLevel(Level.WARN)
    Logger.getLogger("org.apache.spark.scheduler.TaskSetManager").setLevel(Level.WARN)
    Logger.getLogger("akka").setLevel(Level.OFF)
    Logger.getLogger("io.netty").setLevel(Level.OFF)
    Logger.getLogger("org.spark-project.jetty").setLevel(Level.OFF)
    Logger.getLogger("org.apache.hadoop.hdfs.KeyProviderCache").setLevel(Level.OFF)
  }

  private def makeFileSystem(sc: SparkContext) = {
    if (sc.isLocal) FileSystem.getLocal(sc.hadoopConfiguration)
    else FileSystem.get(sc.hadoopConfiguration)
  }

  protected def body()(implicit fs: FileSystem, sparkContext: SparkContext, sqlContext: SQLContext, settings: DQSettings): Boolean

  def preMessage(task:String) = {
    log.warn("************************************************************************")
    log.warn(s"               STARTING EXECUTION OF TASK $task")
    log.warn("************************************************************************")
  }

  def postMessage(task:String) = {
    log.warn("************************************************************************")
    log.warn(s"               FINISHED EXECUTION OF TASK $task")
    log.warn("************************************************************************")
  }

  def main(args: Array[String]) = {

    initLogger()

    val settings = DQSettings.fromArgList(args)
    log.info(s"Loaded settings - ${settings.write}")


    //TODO local as parameter??
    log.info(s"Creating SparkContext, SqlContext and FileSystem")
    val sparkContext = makeSparkContext(settings, isLocal = true)
    val sqlContext   = makeSqlContext(sparkContext)
    val fs           = makeFileSystem(sparkContext)


    preMessage(s"{${settings.appName}}")
    val startTime = System.currentTimeMillis()
    body()(fs, sparkContext, sqlContext, settings)
    postMessage(s"{${settings.appName}}")

    log.info(s"Execution finished in [${(System.currentTimeMillis() - startTime) / 60000}] min(s)")
    log.info("Closing application")
    sparkContext.stop()

    log.info("Spark context terminated ... exiting")
  }

}
