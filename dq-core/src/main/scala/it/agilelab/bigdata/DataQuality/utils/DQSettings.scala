package it.agilelab.bigdata.DataQuality.utils

import java.io.File

import com.typesafe.config._
import it.agilelab.bigdata.DataQuality.exceptions.IllegalParameterException
import it.agilelab.bigdata.DataQuality.sources.DatabaseConfig
import it.agilelab.bigdata.DataQuality.targets.HdfsTargetConfig
import it.agilelab.bigdata.DataQuality.utils
import it.agilelab.bigdata.DataQuality.utils.mailing.MailerConfiguration
import org.apache.log4j.Logger
import org.joda.time
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

import scala.util.Try
import scala.reflect.runtime.universe._


object DQSettings {
  def getConfigOption[T: TypeTag](path: String, conf: Config): Option[T] = {
    val values = typeOf[T] match {
      case x if x =:= typeOf[String] => Try(conf.getString(path)).toOption.filter(_.nonEmpty)
      case x if x =:= typeOf[Int] => Try(conf.getInt(path)).toOption
      case x if x =:= typeOf[Boolean] => Try(conf.getBoolean(path)).toOption
      case _ => None
    }

    values.map(_.asInstanceOf[T])
  }
}

class DQSettings(conf: Config,
                 val configFilePath: String,
                 val repartition: Boolean,
                 val local: Boolean,
                 val ref_date: DateTime) {

  def this(commandLineOpts: DQCommandLineOptions) {
    this(
      ConfigFactory.parseFile(new File(commandLineOpts.applicationConf)).getConfig("data_quality").resolve(),
      commandLineOpts.configFilePath,
      commandLineOpts.repartition,
      commandLineOpts.local,
      new time.DateTime(commandLineOpts.refDate.getTime)
    )
  }

  private val inputArgDateFormat: DateTimeFormatter = DateTimeFormat.forPattern(utils.applicationDateFormat)

  lazy val refDateString: String = ref_date.toString(inputArgDateFormat)

  /* application.conf parameters */
  val appName: String = Try(conf.getString("application_name")).toOption.getOrElse("Data_Quality")

  val s3Bucket: String = Try(conf.getString("s3_bucket")).toOption.getOrElse("")

  val hiveDir: Option[String] = DQSettings.getConfigOption[String]("hive_warehouse_path", conf)
  val hbaseHost: Option[String] = DQSettings.getConfigOption[String]("hbase_host", conf)

  val localTmpPath: Option[String] = DQSettings.getConfigOption[String]("tmp_files_management.local_fs_path", conf)
  val hdfsTmpPath: Option[String] = DQSettings.getConfigOption[String]("tmp_files_management.hdfs_path", conf)
  val tmpFileDelimiter: Option[String] = DQSettings.getConfigOption[String]("tmp_files_management.delimiter", conf)

  // Error managements parameters
  val errorFolderPath: Option[String] = DQSettings.getConfigOption[String]("metric_error_management.dump_directory_path", conf)
  val errorDumpSize: Int =  DQSettings.getConfigOption[Int]("metric_error_management.dump_size", conf).getOrElse(1000)

  val errorFileFormat: String = DQSettings.getConfigOption[String]("metric_error_management.file_config.format", conf).getOrElse("csv")
  val errorFileDelimiter: Option[String] = DQSettings.getConfigOption[String]("metric_error_management.file_config.delimiter", conf)
  val errorFileQuote: Option[String] = DQSettings.getConfigOption[String]("metric_error_management.file_config.quote", conf)
  val errorFileEscape: Option[String] = DQSettings.getConfigOption[String]("metric_error_management.file_config.escape", conf)
  val errorFileQuoteMode: Option[String] = DQSettings.getConfigOption[String]("metric_error_management.file_config.quote_mode", conf)

  // Virtual sources parameters
  val vsDumpConfig: Option[HdfsTargetConfig] = Try {
    val obj: Config = conf.getConfig("virtual_sources_management")
    val path = DQSettings.getConfigOption[String]("dump_directory_path", obj).get
    val fileFormat = DQSettings.getConfigOption[String]("file_format", obj).get
    val delimiter = DQSettings.getConfigOption[String]("delimiter", obj)
    HdfsTargetConfig.apply("vsd", fileFormat, path, delimiter)
  }.toOption

  val mailingMode: Option[String] = DQSettings.getConfigOption[String]("mailing.mode", conf)
  val scriptPath: Option[String] = DQSettings.getConfigOption[String]("mailing.mail_script_path", conf)
  val mailingConfig: Option[MailerConfiguration] = {
    val monfig = Try(conf.getConfig("mailing.conf")).toOption
    monfig match {
      case Some(c) => Try(new MailerConfiguration(c)).toOption
      case None    => None
    }
  }
  val notifications: Boolean = DQSettings.getConfigOption[Boolean]("mailing.notifications", conf).getOrElse(false)

  val resStorage: Option[DatabaseConfig] = conf.getString("storage.type") match {
    case "DB" => Some(new DatabaseConfig(conf.getConfig("storage.config")))
    case "NONE" => None
    case x    => throw IllegalParameterException(x)
  }

  def logThis()(implicit log: Logger): Unit = {
    log.info(s"[CONF] General application configuration:")
    log.info(s"[CONF] - HBase host: ${this.hbaseHost}")
    log.info(s"[CONF] - Hive warehouse path: ${this.hiveDir}")
    log.info(s"[CONF] - Metric error management configuration:")
    log.info(s"[CONF]   - Dump directory path path: ${this.errorFolderPath}")
    log.info(s"[CONF]   - Dump size: ${this.errorDumpSize}")
    log.info(s"[CONF] - Temporary files management configuration:")
    log.info(s"[CONF]   - Local FS path: ${this.localTmpPath}")
    log.info(s"[CONF]   - HDFS path: ${this.hdfsTmpPath}")
    log.info(s"[CONF] - Virtual sources management configuration:")
    log.info(s"[CONF]   - Dump path: ${this.vsDumpConfig.map(_.path)}")
    log.info(s"[CONF]   - File format: ${this.vsDumpConfig.map(_.fileFormat)}")
    log.info(s"[CONF]   - Delimiter: ${this.vsDumpConfig.map(_.delimiter)}")
    log.info(s"[CONF] - Storage configuration:")
    log.info(s"[CONF]   - Mode: ${conf.getString("storage.type")}")
    log.info(s"[CONF] - Mailing configuration:")
    log.info(s"[CONF]   - Mode: ${this.mailingMode}")
    log.info(s"[CONF]   - Script path: ${this.scriptPath}")
    log.info(s"[CONF]   - Notifications: ${this.notifications}")
  }

}
