package it.agilelab.bigdata.DataQuality.checks

import java.text.SimpleDateFormat
import java.util.Calendar

/**
  * Created by Gianvito Siciliano on 29/12/16.
  */

case class CheckResult(
                         checkId: String,
                         checkName: String,
                         checkDescription: String,
                         checkedFile: String,
                         baseMetric: String,
                         comparedMetric: Option[String],
                         comparedThreshold: Double,
                         status: String,
                         message: String,
                         execData:String = {
                           val formatDate = new SimpleDateFormat("yyyy-MM-dd:hhmmss")
                           val now  = Calendar.getInstance().getTime
                           formatDate.format(now)
                         }
                      )