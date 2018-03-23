package it.agilelab.bigdata.DataQuality.checks

import it.agilelab.bigdata.DataQuality.metrics.DQResultTypes.DQResultType
import it.agilelab.bigdata.DataQuality.metrics.{DQResultTypes, TypedResult}

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
  override def getType: DQResultType = DQResultTypes.check
}