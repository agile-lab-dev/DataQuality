package it.agilelab.bigdata.DataQuality.utils.mailing
import it.agilelab.bigdata.DataQuality.apps.DQMasterBatch.log
import it.agilelab.bigdata.DataQuality.checks.{CheckResult, CheckStatusEnum, LoadCheckResult}
import it.agilelab.bigdata.DataQuality.utils.DQSettings

object NotificationManager {

  def sendSummary(summary: Summary, additional: Option[String] = None)(implicit settings: DQSettings): Unit = {
    if (settings.notifications) {
      val text = summary.toMailString() + "\n" + additional.getOrElse("")

      settings.mailingMode match {
        case Some("internal") =>
          import sys.process.stringSeqToProcess
          Seq(
            "/bin/bash",
            settings.scriptPath.get,
            text,
            summary.status
          ) !!

          log.info("Report have been sent.")
        case x => throw new IllegalArgumentException(s"Illegal mailing mode: $x")
      }
    } else log.warn("Notifications are disabled.")
  }

  def saveResultsLocally(summary: Summary,
                         checks: Option[Seq[CheckResult]] = None,
                         lc: Option[Seq[LoadCheckResult]] = None)(implicit settings: DQSettings): Unit = {

    if (settings.localTmpPath.isDefined) {
      val runName: String = settings.appName
      val dirPath = settings.localTmpPath.get + "/" +settings.refDateString + "/" + runName

      import java.io._

      val dir = new File(dirPath)
      dir.mkdirs()

      // summary.csv
      log.info(s"Saving summary file to $dirPath/summary.csv")
      val summaryFile = new File(dirPath + "/" + "summary.csv")
      summaryFile.createNewFile()
      val s_bw = new BufferedWriter(new FileWriter(summaryFile))
      s_bw.write(summary.toCsvString())
      s_bw.close()

      // failed_load_checks.csv
      if(lc.isDefined) {
        log.info(s"Saving failed load checks to $dirPath/failed_load_checks.csv")
        val lcFile = new File(dirPath + "/" + "failed_load_checks.csv")
        lcFile.createNewFile()
        val lc_bw = new BufferedWriter(new FileWriter(lcFile))
        lc_bw.write(lc.get.filter(_.status != CheckStatusEnum.Success).map(_.toCsvString()).mkString("\n"))
        lc_bw.close()
      }

      // failed_metric_checks.csv
      if(checks.isDefined) {
        log.info(s"Saving failed metric checks to $dirPath/failed_metric_checks.csv")
        val chkFile = new File(dirPath + "/" + "failed_metric_checks.csv")
        chkFile.createNewFile()
        val chk_bw = new BufferedWriter(new FileWriter(chkFile))
        chk_bw.write(checks.get.filter(_.status != "Success").map(_.toCsvString()).mkString("\n"))
        chk_bw.close()
      }

      log.info("Local results have been saved.")
    } else log.warn("Local temp path is not defined")
  }

}
