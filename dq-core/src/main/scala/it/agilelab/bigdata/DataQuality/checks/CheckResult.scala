package it.agilelab.bigdata.DataQuality.checks

import it.agilelab.bigdata.DataQuality.checks.CheckStatusEnum.CheckResultStatus
import it.agilelab.bigdata.DataQuality.metrics.DQResultTypes.DQResultType
import it.agilelab.bigdata.DataQuality.metrics.{DQResultTypes, TypedResult}
import it.agilelab.bigdata.DataQuality.utils.DQSettings

/**
  * Created by Gianvito Siciliano on 29/12/16.
  *
  * Representation of check result
  */
case class CheckResult(
    checkId: String,
    checkName: String,
    description: String,
    checkedFile: String,
    baseMetric: String,
    comparedMetric: Option[String],
    comparedThreshold: Double,
    status: String,
    message: String,
    execDate: String
) extends TypedResult {

  def toCsvString()(implicit settings: DQSettings): String = {
    Seq(
      this.checkId,
      this.status.toString,
      this.execDate,
      this.checkName,
      this.baseMetric
    ).mkString(settings.tmpFileDelimiter.getOrElse(","))
  }

  override def getType: DQResultType = DQResultTypes.check
}

case class LoadCheckResult(
    id: String,
    src: String,
    tipo: String,
    expected: String,
    date: String,
    status: CheckResultStatus,
    message: String = ""
) extends TypedResult {
  override def getType: DQResultType = DQResultTypes.load

  def simplify(): LoadCheckResultSimple = LoadCheckResultSimple(
    this.id,
    this.src,
    this.tipo,
    this.expected,
    this.date,
    this.status.toString,
    this.message
  )

  def toCsvString()(implicit settings: DQSettings): String = {
    Seq(this.id, this.status.toString, this.date, this.tipo, this.src)
      .mkString(settings.tmpFileDelimiter.getOrElse(","))
  }
}

// TODO: Find a smarter way to solve issue with saving
case class LoadCheckResultSimple(
    id: String,
    src: String,
    tipo: String,
    expected: String,
    date: String,
    status: String,
    message: String
)
