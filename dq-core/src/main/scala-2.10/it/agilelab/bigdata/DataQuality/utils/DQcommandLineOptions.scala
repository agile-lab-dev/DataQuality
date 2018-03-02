package it.agilelab.bigdata.DataQuality.utils

import java.util.{Calendar, Date}

import scopt.OptionParser

case class DQcommandLineOptions(configFilePath: String = "",
                                refDate: Date = new Date(),
                                repartition: Boolean = false,
                                local: Boolean = false)

object DQcommandLineOptions {

  def parser() = new OptionParser[DQcommandLineOptions]("dataquality") {
    opt[String]('c', "configFilePath") required () action { (x, c) =>
      c.copy(configFilePath = x)
    } text "Path to run configuration file"
    opt[Calendar]('d', "refDate") optional () action { (x, c) =>
      c.copy(refDate = x.getTime)
    } text "Indicates the date at which the DataQuality checks will be performed"
    opt[Unit]('r', "repartition") optional () action { (_, c) =>
      c.copy(repartition = true)
    } text "Specifies whether the application is repartitioning the input data"
    opt[Unit]('l', "local") optional () action { (_, c) =>
      c.copy(local = true)
    } text "Specifies whether the application is operating in local mode"
  }

}
