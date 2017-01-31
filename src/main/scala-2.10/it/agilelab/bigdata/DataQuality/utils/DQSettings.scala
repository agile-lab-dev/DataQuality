package it.agilelab.bigdata.DataQuality.utils

import java.io.PrintWriter

import org.apache.commons.cli.{BasicParser, CommandLine, HelpFormatter, Options, Option => CliOption}
import org.joda.time
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat

import scala.util.{Failure, Success, Try}

/**
  * Created by Paolo on 20/01/2017.
  */

class DQSettings(
                  val appName: String,
                  val configFilePath: String,
                  val refDateString: String,
                  val hadoopConfDir: String,
                  val debug: Boolean
                ) {

  def this(obj: DQSettings) = this(
    obj.appName,
    obj.configFilePath,
    obj.refDateString,
    obj.hadoopConfDir,
    obj.debug)

  val inputArgDateFormat = DateTimeFormat.forPattern("yyyy-MM-dd").withZone(DateTimeZone.UTC)

  val ref_date = DateTime.parse(refDateString, inputArgDateFormat)

  def argsToWrite: Seq[(String, Any)] =
    "appName" -> appName ::
    "configFilePath" -> configFilePath ::
    "refDateString" -> refDateString ::
    "hadoopConfDir" -> hadoopConfDir ::
    "debug" -> debug ::
    Nil

  def write = {
    ("-----------" +:
      argsToWrite.map(v => s"${v._1} = ${v._2}") :+
      "-----------").mkString("\n")
  }

  def dump = println(write)


}

trait SettingsFactory[S <: DQSettings] {
  def fromArgList(args: Array[String]): S
}


object DQSettings extends SettingsFactory[DQSettings] {

  object Options {
    def appName    = new CliOption("n", "name", true, "Spark job name")

    def configPath = new CliOption("c", "config", true, "Path to configuration file")

    def refDate    = new CliOption("r", "refDate", true, "Indicates the date at which the DataQuality checks will be performed")

    def debugMode  = new CliOption("d", "debug", false, "Specifies whether the application is operating under debugging conditions")

    def hadoopPath = new CliOption("h", "hadoop", true, "Path to hadoop configuration")

    def all = Seq(
      appName,
      configPath,
      refDate,
      debugMode,
      hadoopPath
    )
  }

  private def makeSettings(cli: CommandLine) = {

    new DQSettings(
      cli.getOptionValue(Options.appName.getOpt, "DQ"),
      cli.getOptionValue(Options.configPath.getOpt, ""),
      cli.getOptionValue(Options.refDate.getOpt, (new time.DateTime()).toString("yyyy-MM-dd")),
      cli.getOptionValue(Options.hadoopPath.getOpt, "/etc/hadoop/conf"),
      cli.hasOption(Options.debugMode.getOpt)
    )
  }

  def fromArgList(args: Array[String]) = makeSettings(CliUtils.parseArgsList(args, Options.all))

  def fromArgListWithOptions(appName: String, args: Array[String], options: Seq[CliOption]) = makeSettings(CliUtils.parseArgsList(args, options))
}


object CliUtils {
  def parseArgsList(args: Array[String], options: Seq[CliOption]) = {
    val opts = options.foldLeft(new Options) {
      _ addOption _
    }
    Try {
      new BasicParser().parse(opts, args)
    } match {
      case Success(settings) => settings
      case Failure(e) => printHelp("DQ", opts); throw e
    }
  }

  def printHelp(appName: String, options: org.apache.commons.cli.Options) {
    val sysOut = new PrintWriter(System.out)
    new HelpFormatter().printUsage(sysOut, 100, appName, options)
    sysOut.close()
  }
}