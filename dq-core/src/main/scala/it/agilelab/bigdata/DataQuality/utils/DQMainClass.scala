package it.agilelab.bigdata.DataQuality.utils

import java.util.Locale

import com.typesafe.config.ConfigFactory
import it.agilelab.bigdata.DataQuality.utils.io.LocalDBManager
import org.apache.hadoop.fs.FileSystem
import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext

/**
  * Created by Paolo on 20/01/2017.
  */
trait DQMainClass { this: DQSparkContext with Logging =>

  private def initLogger(): Unit = {
    Logger.getLogger("org").setLevel(Level.WARN)
    Logger
      .getLogger("org.apache.spark.scheduler.TaskSetManager")
      .setLevel(Level.WARN)
    Logger
      .getLogger("org.apache.spark.scheduler.TaskSetManager")
      .setLevel(Level.WARN)
    Logger.getLogger("akka").setLevel(Level.OFF)
    Logger.getLogger("io.netty").setLevel(Level.OFF)
    Logger.getLogger("org.spark-project.jetty").setLevel(Level.OFF)
    Logger
      .getLogger("org.apache.hadoop.hdfs.KeyProviderCache")
      .setLevel(Level.OFF)
  }

  private def makeFileSystem(sc: SparkContext) = {
    if (sc.isLocal) FileSystem.getLocal(sc.hadoopConfiguration)
    else FileSystem.get(sc.hadoopConfiguration)
  }

  protected def body()(implicit fs: FileSystem,
                       sparkContext: SparkContext,
                       sqlContext: SQLContext,
                       sqlWriter: LocalDBManager,
                       settings: DQSettings): Boolean

  def preMessage(task: String) = {
    log.warn(
      "************************************************************************")
    log.warn(s"               STARTING EXECUTION OF TASK $task")
    log.warn(
      "************************************************************************")
  }

  def postMessage(task: String) = {
    log.warn(
      "************************************************************************")
    log.warn(s"               FINISHED EXECUTION OF TASK $task")
    log.warn(
      "************************************************************************")
  }

  def main(args: Array[String]) = {

    // set to avoid casting problems in metric result name generation
    Locale.setDefault(Locale.ENGLISH)

    initLogger()

    DQcommandLineOptions.parser().parse(args, DQcommandLineOptions()) match {
      case Some(dQcommandLineOptions) =>
        // Load our own config values from the default location, application.conf
        val conf = ConfigFactory.load()
        val settings =
          DQSettings(dQcommandLineOptions, conf.getConfig("dataquality"))

        log.info("APP_DIR: " + settings.appDir)
        log.info("Mailing mode: " + settings.mailingMode)
        settings.mailingConfig match {
          case Some(mconf) => log.info("With configuration: " + mconf.toString)
          case None        =>
        }

        log.info(s"Creating SparkContext, SqlContext and FileSystem")
        // initialize heare because they will be used in every run
        val sparkContext = makeSparkContext(settings)
        val sqlContext = makeSqlContext(sparkContext)
        val fs = makeFileSystem(sparkContext)
        val localSqlWriter = new LocalDBManager(settings)

        preMessage(s"{${settings.appName}}")
        val startTime = System.currentTimeMillis()
        body()(fs, sparkContext, sqlContext, localSqlWriter, settings)
        postMessage(s"{${settings.appName}}")

        log.info(
          s"Execution finished in [${(System.currentTimeMillis() - startTime) / 60000}] min(s)")
        log.info("Closing application")

        localSqlWriter.closeConnection()
        sparkContext.stop()

        log.info("Spark context terminated ... exiting")

      case None =>
        log.error("WRONG PARAMS")
        throw new Exception(" WRONG PARAMS")

    }

  }

}
