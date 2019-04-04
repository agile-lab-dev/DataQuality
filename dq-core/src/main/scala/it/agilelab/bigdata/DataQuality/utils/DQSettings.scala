package it.agilelab.bigdata.DataQuality.utils

import java.io.File

import com.typesafe.config._
import it.agilelab.bigdata.DataQuality.exceptions.IllegalParameterException
import it.agilelab.bigdata.DataQuality.sources.DatabaseConfig
import it.agilelab.bigdata.DataQuality.targets.HdfsTargetConfig
import it.agilelab.bigdata.DataQuality.utils
import org.joda.time
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

import scala.util.Try

/**
  * Created by Paolo on 20/01/2017.
  */
class DQSettings(conf: Config,
                 val configFilePath: String,
                 val repartition: Boolean,
                 val local: Boolean,
                 val ref_date: DateTime) {

  def this(commandLineOpts: DQCommandLineOptions) {
    this(
      ConfigFactory.parseFile(new File(commandLineOpts.applicationConf)).getConfig("dataquality").resolve(),
      commandLineOpts.configFilePath,
      commandLineOpts.repartition,
      commandLineOpts.local,
      new time.DateTime(commandLineOpts.refDate.getTime)
    )
  }

  private val inputArgDateFormat: DateTimeFormatter =
    DateTimeFormat.forPattern("yyyy-MM-dd")

  lazy val refDateString: String = ref_date.toString(inputArgDateFormat)

  /* application.conf parameters */

  val vsDumpConfig: Option[HdfsTargetConfig] = Try {
    val obj: Config = conf.getConfig("vsDumpConfig")
    utils.parseTargetConfig(obj).get
  }.toOption

  val appName: String = Try(conf.getString("appName")).toOption.getOrElse("")
  val appDir: String = Try(conf.getString("appDirectory")).toOption.getOrElse("")
  val errorDumpSize: Int = Try(conf.getInt("errorDumpSize")).toOption.getOrElse(1000)
  val errorFolderPath: Option[String] = Try(conf.getString("errorFolderPath")).toOption
  val hiveDir: String = Try(conf.getString("hiveDir")).toOption.getOrElse("")
  val hbaseHost: String = Try(conf.getString("hbaseDir")).toOption.getOrElse("")
  val hadoopConfDir: String = Try(conf.getString("hadoopConfDir")).toOption.getOrElse("")
  val mailingMode: String = Try(conf.getString("mailing.mode").toLowerCase).getOrElse("internal")
  val mailingConfig: Option[Mailer] = {
    val monfig = Try(conf.getConfig("mailing.conf")).toOption
    monfig match {
      case Some(c) => Try(new Mailer(c)).toOption
      case None    => None
    }
  }
  private val storageType: String = conf.getString("storage.type")
  private val storageConfig: Config = conf.getConfig("storage.config")
  // todo add new storage types
  val resStorage: Product = storageType match {
    case "DB" => new DatabaseConfig(storageConfig)
    case x    => throw IllegalParameterException(x)
  }
}

case class Mailer(
                   address: String,
                   hostName: String,
                   username: String,
                   password: String,
                   smtpPortSSL: Int,
                   sslOnConnect: Boolean
                 ) {
  def this(config: Config) = {
    this(
      config.getString("address"),
      config.getString("hostname"),
      Try(config.getString("username")).getOrElse(""),
      Try(config.getString("password")).getOrElse(""),
      Try(config.getInt("smtpPort")).getOrElse(465),
      Try(config.getBoolean("sslOnConnect")).getOrElse(true)
    )
  }
}
