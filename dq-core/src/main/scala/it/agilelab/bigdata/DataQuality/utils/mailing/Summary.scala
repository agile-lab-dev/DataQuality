package it.agilelab.bigdata.DataQuality.utils.mailing
import it.agilelab.bigdata.DataQuality.checks.{CheckResult, CheckStatusEnum, LoadCheckResult}
import it.agilelab.bigdata.DataQuality.configs.ConfigReader
import it.agilelab.bigdata.DataQuality.utils.DQSettings

case class Summary(
    sources: Int,
    metrics: Int,
    composed_metrics: Int,
    load_checks: Int,
    checks: Int,
    failed_load_checks: Option[Int],
    failed_checks: Option[Int]
) {

  def this(conf: ConfigReader, checks: Option[Seq[CheckResult]] = None, lc: Option[Seq[LoadCheckResult]] = None) {
    this(
      sources = conf.sourcesConfigMap.size,
      metrics = conf.metricsBySourceList.size,
      composed_metrics = conf.composedMetrics.length,
      load_checks = conf.loadChecksMap.values.foldLeft(0)(_ + _.size),
      checks = conf.metricsByChecksList.size,
      failed_load_checks = lc.map(x => x.count(_.status != CheckStatusEnum.Success)),
      failed_checks = checks.map(x => x.count(_.status != "Success"))
    )
  }

  val status: String = (failed_checks, failed_load_checks) match {
    case (Some(x), Some(y)) => if (x + y == 0) "OK" else "KO"
    case _                  => "ERROR"
  }

  // Status is appended in the send_mail script with the log file path
  def toMailString()(implicit settings: DQSettings): String = s"""Reference date: ${settings.refDateString}
                                     |Run configuration path: ${settings.configFilePath}
                                     |Output location (HDFS): ${settings.hdfsTmpPath.getOrElse("")}
                                     |
                                     |Number of sources: $sources
                                     |Number of metrics: $metrics
                                     |Number of composed metrics: $composed_metrics
                                     |Number of load checks: $load_checks
                                     |Number of metric checks: $checks
                                     |
                                     |Failed load checks: ${failed_load_checks.getOrElse("null")}
                                     |Failed checks: ${failed_checks.getOrElse("null")}
    """.stripMargin

  def toCsvString()(implicit settings: DQSettings): String = {
    val runName: String = settings.appName
    Seq(
      runName,
      status,
      settings.refDateString,
      sources,
      load_checks,
      metrics,
      composed_metrics,
      checks,
      failed_checks.getOrElse(""),
      failed_load_checks.getOrElse(""),
      settings.configFilePath,
      settings.hdfsTmpPath.getOrElse("")
    ).mkString(settings.tmpFileDelimiter.getOrElse(","))
  }

}
