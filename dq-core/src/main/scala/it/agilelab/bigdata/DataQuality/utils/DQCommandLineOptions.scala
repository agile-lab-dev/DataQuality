package it.agilelab.bigdata.DataQuality.utils

import java.util.{Calendar, Date}
import org.joda.time
import org.joda.time.DateTime

import scopt.OptionParser

case class DQCommandLineOptions(applicationConf: String,
                                configFilePath: String,
                                refDate: Date = new Date(),
                                repartition: Boolean = false,
                                local: Boolean = false)

object DQCommandLineOptions {

  def parser(): OptionParser[DQCommandLineOptions] =
    new OptionParser[DQCommandLineOptions]("dataquality") {

      opt[String]('a', "application-conf") required () action { (x, c) =>
        c.copy(applicationConf = x)
      } text "Path to application configuration file"

      opt[String]('c', "configFilePath") required () action { (x, c) =>
        c.copy(configFilePath = x)
      } text "Path to run configuration file"

      opt[Calendar]('d', "reference-date") required () action { (x, c) =>
        c.copy(refDate = x.getTime)
      } text "Indicates the date at which the DataQuality checks will be performed (format YYYY-MM-DD)"

      opt[Unit]('r', "repartition") optional () action { (_, c) =>
        c.copy(repartition = true)
      } text "Specifies whether the application is repartitioning the input data"

      opt[Unit]('l', "local") optional () action { (_, c) =>
        c.copy(local = true)
      } text "Specifies whether the application is operating in local mode"
    }
}
