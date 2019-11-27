package it.agilelab.bigdata.DataQuality.utils

import java.util.Locale

import it.agilelab.bigdata.DataQuality.utils.io.HistoryDBManager
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.hive.HiveContext

/**
  * Created by Paolo on 20/01/2017.
  */
trait DQMainClass { this: DQSparkContext with Logging =>

  private def initLogger(): Unit = {
    Logger.getLogger("org").setLevel(Level.WARN)
    Logger.getLogger("org.apache.spark.scheduler.TaskSetManager").setLevel(Level.WARN)
    Logger.getLogger("akka").setLevel(Level.OFF)
    Logger.getLogger("io.netty").setLevel(Level.OFF)
    Logger.getLogger("org.spark-project.jetty").setLevel(Level.OFF)
    Logger.getLogger("org.apache.hadoop.hdfs.KeyProviderCache").setLevel(Level.OFF)
  }

  private def makeFileSystem(settings: DQSettings, sc: SparkContext): FileSystem = {
    if (sc.isLocal) FileSystem.getLocal(sc.hadoopConfiguration)
    else{

      if (!settings.s3Bucket.isEmpty) {
        sc.hadoopConfiguration.set("fs.defaultFS", settings.s3Bucket)
        sc.hadoopConfiguration.set("fs.AbstractFileSystem.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
      }

      FileSystem.get( sc.hadoopConfiguration)

    }
  }

  protected def body()(implicit fs: FileSystem,
                       sparkContext: SparkContext,
                       sqlContext: SQLContext,
                       sqlWriter: HistoryDBManager,
                       settings: DQSettings): Boolean

  def preMessage(task: String): Unit = {
    log.warn("************************************************************************")
    log.warn(s"               Starting execution of task $task")
    log.warn("************************************************************************")
  }

  def postMessage(task: String): Unit = {
    log.warn("************************************************************************")
    log.warn(s"               Finishing execution of task $task")
    log.warn("************************************************************************")
  }

  def main(args: Array[String]): Unit = {
    // set to avoid casting problems in metric result name generation
    Locale.setDefault(Locale.ENGLISH)
    initLogger()

    DQCommandLineOptions.parser().parse(args, DQCommandLineOptions("","")) match {
      case Some(commandLineOptions) =>
        // Load our own config values from the default location, application.conf
        val settings = new DQSettings(commandLineOptions)
        val sparkContext = makeSparkContext(settings)
        val fs = makeFileSystem(settings, sparkContext)

        settings.logThis()(log)

        val sqlContext: SQLContext = if (settings.hiveDir.isDefined) {
          val hc =  new HiveContext(sparkContext)
          hc.setConf("hive.metastore.warehouse.dir", settings.hiveDir.get)
          hc
        } else makeSqlContext(sparkContext)

        val historyDatabase = new HistoryDBManager(settings)

        // Starting application body
        preMessage(s"{Data Quality ${settings.appName}}")
        val startTime = System.currentTimeMillis()
        body()(fs, sparkContext, sqlContext, historyDatabase, settings)
        postMessage(s"{Data Quality ${settings.appName}}")

        log.info(s"Execution finished in [${(System.currentTimeMillis() - startTime) / 60000}] min(s)")
        log.info("Closing application...")

        historyDatabase.closeConnection()
        sparkContext.stop()

        log.info("Spark context were terminated. Exiting...")

      case None =>
        log.error("Wrong parameters provided")
        throw new Exception("Wrong parameters provided")

    }

  }

}
