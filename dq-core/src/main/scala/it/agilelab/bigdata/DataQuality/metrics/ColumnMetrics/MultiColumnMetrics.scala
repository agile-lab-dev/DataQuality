package it.agilelab.bigdata.DataQuality.metrics.ColumnMetrics

import it.agilelab.bigdata.DataQuality.metrics.CalculatorStatus.CalculatorStatus
import it.agilelab.bigdata.DataQuality.metrics.MetricProcessor.ParamMap
import it.agilelab.bigdata.DataQuality.metrics.{CalculatorStatus, MetricCalculator, StatusableCalculator}
import it.agilelab.bigdata.DataQuality.utils.{Logging, getParametrizedMetricTail, tryToString}
import org.apache.commons.lang3.StringUtils
import org.joda.time.Days
import org.joda.time.format.DateTimeFormat

import scala.util.{Success, Try}

object MultiColumnMetrics extends Logging {

  case class EqualStringColumnsMetricCalculator(
      cnt: Int,
      protected val status: CalculatorStatus = CalculatorStatus.OK,
      protected val failCount: Int = 0)
      extends StatusableCalculator {

    def this(paramMap: Map[String, Any]) {
      this(0)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      Try {
        val firstVal = tryToString(values.head)
        val secondVal = tryToString(values(1))

        val incrementedState =
          (firstVal, secondVal) match {
            case (Some(x), Some(y)) if x == y =>
              EqualStringColumnsMetricCalculator(cnt = this.cnt + 1,
                                                 status = CalculatorStatus.OK,
                                                 failCount = this.failCount)
            case _ => copyWithState(CalculatorStatus.FAILED)
          }
        incrementedState
      }.getOrElse(copyWithState(CalculatorStatus.FAILED))

    }

    override def result(): Map[String, (Double, Option[String])] =
      Map("COLUMN_EQ" -> (cnt.toDouble, None))
    override def merge(m2: MetricCalculator): MetricCalculator = {
      val m2Casted = m2.asInstanceOf[EqualStringColumnsMetricCalculator]
      EqualStringColumnsMetricCalculator(
        this.cnt + m2Casted.cnt,
        this.status,
        this.getFailCounter + m2Casted.getFailCounter
      )
    }
    override protected def copyWithState(failed: CalculatorStatus)
      : MetricCalculator with StatusableCalculator = {
      this.copy(status = failed, failCount = this.failCount + 1)
    }

  }

  /**
    * calculate the number of the rows for which the day difference btw
    * the two columns given as input is les than the threshold: day-treshold"
    *   "FORMATTED_DATE"
    */
  case class DayDistanceMetric(cnt: Double,
                               paramMap: ParamMap,
                               protected val status: CalculatorStatus =
                                 CalculatorStatus.OK,
                               protected val failCount: Int = 0)
      extends StatusableCalculator {

    private val formatDate: String = paramMap("dateFormat").toString
    private val dayThreshold: Int = paramMap("threshold")
      .asInstanceOf[Double]
      .toInt

    def this(paramMap: Map[String, Any]) {
      this(0, paramMap)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      Try {
        val firstVal = tryToString(values.head)
        val secondVal = tryToString(values(1))
        val fmt = DateTimeFormat forPattern formatDate

        val result: Boolean = (firstVal, secondVal) match {
          case (Some(dateString1), Some(dateString2)) =>
            val trydateA = Try(fmt parseDateTime dateString1)
            val trydateB = Try(fmt parseDateTime dateString2)
            (trydateA, trydateB) match {
              case (Success(dateA), Success(dateB)) =>
                if (Math.abs(Days.daysBetween(dateA, dateB).getDays) < dayThreshold)
                  true
                else false
              case _ => false
            }
          case _ => false
        }
        if (result)
          DayDistanceMetric(
            cnt = this.cnt + 1,
            paramMap = this.paramMap,
            status = CalculatorStatus.OK,
            failCount = this.failCount
          )
        else copyWithState(CalculatorStatus.FAILED)

      }.getOrElse(this)
    }

    override def result(): Map[String, (Double, Option[String])] =
      Map("DAY_DISTANCE" + getParametrizedMetricTail(paramMap) -> (cnt, None))

    override def merge(m2: MetricCalculator): MetricCalculator = {
      val m2Casted = m2.asInstanceOf[DayDistanceMetric]
      DayDistanceMetric(
        this.cnt + m2Casted.cnt,
        paramMap,
        status,
        this.getFailCounter + m2Casted.getFailCounter
      )
    }

    override protected def copyWithState(
        failed: CalculatorStatus): MetricCalculator with StatusableCalculator =
      this.copy(status = failed, failCount = this.failCount + 1)
  }

  case class LevenshteinDistanceMetric(cnt: Double,
                                       paramMap: ParamMap,
                                       protected val status: CalculatorStatus =
                                         CalculatorStatus.OK,
                                       protected val failCount: Int = 0)
      extends StatusableCalculator {

    private val distanceThreshold: Double = paramMap("threshold")
      .asInstanceOf[Double]

    def this(paramMap: Map[String, Any]) {
      this(0, paramMap)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      Try {
        val firstVal = tryToString(values.head)
        val secondVal = tryToString(values(1))

        val result: Boolean = (firstVal, secondVal) match {
          case (Some(x), Some(y)) =>
            val cleanX = x.trim().toUpperCase
            val cleanY = y.trim().toUpperCase

            val normalization = math.max(cleanX.length, cleanY.length)
            val distance = StringUtils.getLevenshteinDistance(cleanX, cleanY) / normalization.toDouble

            distance <= distanceThreshold
          case _ => false
        }
        if (result)
          LevenshteinDistanceMetric(cnt = this.cnt + 1,
                                    paramMap = this.paramMap,
                                    failCount = this.failCount)
        else copyWithState(CalculatorStatus.FAILED)

      }.getOrElse(this)

    }

    override def result(): Map[String, (Double, Option[String])] =
      Map(
        "LEVENSHTEIN_DISTANCE" + getParametrizedMetricTail(paramMap) -> (cnt, None))

    override def merge(m2: MetricCalculator): MetricCalculator = {
      val other = m2.asInstanceOf[LevenshteinDistanceMetric]
      LevenshteinDistanceMetric(this.cnt + other.cnt,
                                paramMap,
                                this.status,
                                this.failCount + other.failCount)
    }

    override protected def copyWithState(
        failed: CalculatorStatus): MetricCalculator with StatusableCalculator =
      this.copy(status = failed, failCount = this.failCount + 1)
  }

}
