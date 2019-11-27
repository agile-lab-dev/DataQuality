package it.agilelab.bigdata.DataQuality.metrics.ColumnMetrics

import java.text.SimpleDateFormat

import it.agilelab.bigdata.DataQuality.metrics.CalculatorStatus.CalculatorStatus
import it.agilelab.bigdata.DataQuality.metrics.MetricProcessor.ParamMap
import it.agilelab.bigdata.DataQuality.metrics.{CalculatorStatus, MetricCalculator, StatusableCalculator}
import it.agilelab.bigdata.DataQuality.utils.{getParametrizedMetricTail, _}
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat

import scala.util.Try

/**
  * Created by Egor Makhov on 29/05/2017.
  *
  * Basic metrics that can be applied to string (or string like) elements
  */
object BasicStringMetrics {

  /**
    * Calculates count of distinct values in processed elements
    * WARNING: Uses set without any kind of trimming and hashing. Return the exact count.
    * So if you a big diversion of elements and does not need an exact result,
    * it's better to use HyperLogLog version (called with "APPROXIMATE_DISTINCT_VALUES").
    * @param uniqueValues Set of processed values
    *
    * @return result map with keys:
    *   "DISTINCT_VALUES"
    */
  case class UniqueValuesMetricCalculator(uniqueValues: Set[Any])
      extends MetricCalculator {

    def this(paramMap: Map[String, Any]) {
      this(Set.empty[Any])
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      tryToString(values.head) match {
        case Some(v) => UniqueValuesMetricCalculator(uniqueValues + v)
        case None    => this
      }
    }

    override def result(): Map[String, (Double, Option[String])] =
      Map("DISTINCT_VALUES" -> (uniqueValues.size.toDouble, None))

    override def merge(m2: MetricCalculator): MetricCalculator =
      UniqueValuesMetricCalculator(
        this.uniqueValues ++ m2
          .asInstanceOf[UniqueValuesMetricCalculator]
          .uniqueValues)

  }

  /**
    * Calculates amount of rows that fits the provided regular expression
    * @param cnt current counter
    * @param paramMap should contain regex
    */
  case class RegexValuesMetricCalculator(cnt: Int, paramMap: Map[String, Any])
      extends MetricCalculator {

    private val regex: String = paramMap("regex").toString

    def this(paramMap: Map[String, Any]) {
      this(0, paramMap)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      tryToString(values.head) match {
        case Some(x) if x.matches(regex) => this.copy(cnt + 1)
        case _                           => this
      }
    }

    override def result(): Map[String, (Double, Option[String])] =
      Map("REGEX_VALUES" + getParametrizedMetricTail(paramMap) -> (cnt.toDouble, None))

    override def merge(m2: MetricCalculator): MetricCalculator =
      RegexValuesMetricCalculator(
        this.cnt + m2.asInstanceOf[RegexValuesMetricCalculator].cnt,
        paramMap)
  }

  /**
    * Calculates amount of null values in processed elements
    * @param cnt Current amount of null values
    *
    * @return result map with keys:
    *   "NULL_VALUES"
    */
  case class NullValuesMetricCalculator(cnt: Int,
                                        protected val status: CalculatorStatus =
                                          CalculatorStatus.OK,
                                        protected val failCount: Int = 0)
      extends StatusableCalculator {

    def this(paramMap: Map[String, Any]) {
      this(0)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      if (values.head == null) {
        NullValuesMetricCalculator(cnt + 1, CalculatorStatus.OK, this.failCount)
      } else copyWithState(CalculatorStatus.FAILED)
    }

    override def result(): Map[String, (Double, Option[String])] =
      Map("NULL_VALUES" -> (cnt.toDouble, None))

    override def merge(m2: MetricCalculator): MetricCalculator = {
      val m2Casted = m2.asInstanceOf[NullValuesMetricCalculator]
      NullValuesMetricCalculator(
        this.cnt + m2.asInstanceOf[NullValuesMetricCalculator].cnt,
        this.status,
        this.getFailCounter + m2Casted.getFailCounter)
    }

    override protected def copyWithState(failed: CalculatorStatus)
      : MetricCalculator with StatusableCalculator = {
      this.copy(status = failed, failCount = this.failCount + 1)
    }

  }

  /**
    * Calculates amount of empty strings in processed elements
    * @param cnt Current amount of empty strings
    *
    * @return result map with keys:
    *   "EMPTY_VALUES"
    */
  case class EmptyStringValuesMetricCalculator(cnt: Int)
      extends MetricCalculator {

    def this(paramMap: Map[String, Any]) {
      this(0)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      EmptyStringValuesMetricCalculator(
        cnt + (if (values.head
                     .isInstanceOf[String] && values.head.toString == "") 1
               else 0))
    }

    override def result(): Map[String, (Double, Option[String])] =
      Map("EMPTY_VALUES" -> (cnt.toDouble, None))

    override def merge(m2: MetricCalculator): MetricCalculator =
      EmptyStringValuesMetricCalculator(
        this.cnt + m2.asInstanceOf[EmptyStringValuesMetricCalculator].cnt)

  }

  /**
    * Calculates minimal length of processed elements
    * @param strl Current minimal string length
    *
    * @return result map with keys:
    *   "MIN_STRING"
    */
  case class MinStringValueMetricCalculator(strl: Int)
      extends MetricCalculator {

    def this(paramMap: Map[String, Any]) {
      this(0)
    }
    override def increment(values: Seq[Any]): MetricCalculator = {
      tryToString(values.head) match {
        case Some(v) => MinStringValueMetricCalculator(Math.min(v.length, strl))
        case None    => this
      }
    }

    override def result(): Map[String, (Double, Option[String])] =
      Map("MIN_STRING" -> (strl.toDouble, None))

    override def merge(m2: MetricCalculator): MetricCalculator =
      MinStringValueMetricCalculator(
        Math.min(this.strl,
                 m2.asInstanceOf[MinStringValueMetricCalculator].strl))

  }

  /**
    * Calculates maximal length of processed elements
    * @param strl Current maximal string length
    *
    * @return result map with keys:
    *   "MAX_STRING"
    */
  case class MaxStringValueMetricCalculator(strl: Double)
      extends MetricCalculator {

    def this(paramMap: Map[String, Any]) {
      this(0)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      tryToString(values.head) match {
        case Some(v) => MaxStringValueMetricCalculator(Math.max(v.length, strl))
        case None    => this
      }
    }

    override def result(): Map[String, (Double, Option[String])] =
      Map("MAX_STRING" -> (strl, None))

    override def merge(m2: MetricCalculator): MetricCalculator =
      MaxStringValueMetricCalculator(
        Math.max(this.strl,
                 m2.asInstanceOf[MaxStringValueMetricCalculator].strl))

  }

  /**
    * Calculates average length of processed elements
    * @param sum Current sum of lengths
    * @param cnt Current count of elements
    *
    * @return result map with keys:
    *   "AVG_STRING"
    */
  case class AvgStringValueMetricCalculator(sum: Double, cnt: Int)
      extends MetricCalculator {

    def this(paramMap: Map[String, Any]) {
      this(0, 0)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      tryToString(values.head) match {
        case Some(v) => AvgStringValueMetricCalculator(sum + v.length, cnt + 1)
        case None    => this
      }
    }

    override def result(): Map[String, (Double, Option[String])] =
      Map("AVG_STRING" -> (sum / cnt.toDouble, None))

    override def merge(m2: MetricCalculator): MetricCalculator = {
      val cm2 = m2.asInstanceOf[AvgStringValueMetricCalculator]
      AvgStringValueMetricCalculator(
        this.sum + cm2.sum,
        this.cnt + cm2.cnt
      )
    }

  }

  /**
    * Calculates amount of strings in provided date format
    * @param cnt Current count of filtered elements
    * @param paramMap Required configuration map. May contains:
    *   required "dateFormat" - requested date format
    *
    * @return result map with keys:
    *   "FORMATTED_DATE"
    */
  case class DateFormattedValuesMetricCalculator(cnt: Double,
                                                 paramMap: ParamMap)
      extends MetricCalculator {

    private val formatDate: String = paramMap("dateFormat").toString

    def this(paramMap: Map[String, Any]) {
      this(0, paramMap)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      if (checkDate(values.head, formatDate))
        DateFormattedValuesMetricCalculator(cnt + 1, paramMap)
      else this
    }

    override def result(): Map[String, (Double, Option[String])] =
      Map("FORMATTED_DATE" + getParametrizedMetricTail(paramMap) -> (cnt, None))

    override def merge(m2: MetricCalculator): MetricCalculator =
      DateFormattedValuesMetricCalculator(
        this.cnt + m2.asInstanceOf[DateFormattedValuesMetricCalculator].cnt,
        paramMap)

    private def checkDate(value: Any, dateFormat: String) = {
      tryToString(value) match {
        case Some(v) =>
          val joda = Try(LocalDateTime.parse(v, DateTimeFormat.forPattern(dateFormat))).isSuccess
          val sdf = Try(new SimpleDateFormat(dateFormat).parse(v)).isSuccess
          joda || sdf
        case _       => false
      }

    }
  }

  /**
    * Calculates amount of strings with specific requested length
    * @param cnt Current count of filtered elements
    * @param paramMap Required configuration map. May contains:
    *   required "length" - requested length
    *
    * @return result map with keys:
    *   "FORMATTED_STRING"
    */
  case class StringFormattedValuesMetricCalculator(cnt: Double,
                                                   paramMap: ParamMap)
      extends MetricCalculator {

    private val length: Int = paramMap("length").toString.toInt

    def this(paramMap: Map[String, Any]) {
      this(0, paramMap)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      tryToString(values.head) match {
        case Some(v) =>
          if (v.length <= length)
            StringFormattedValuesMetricCalculator(cnt + 1, paramMap)
          else this
        case None => this
      }
    }

    override def result(): Map[String, (Double, Option[String])] =
      Map(
        "FORMATTED_STRING" + getParametrizedMetricTail(paramMap) -> (cnt, None))

    override def merge(m2: MetricCalculator): MetricCalculator =
      StringFormattedValuesMetricCalculator(
        this.cnt + m2.asInstanceOf[StringFormattedValuesMetricCalculator].cnt,
        paramMap)

  }

  /**
    * Caclulates amount of string from provided domain
    * @param cnt Current count of filtered elements
    * @param paramMap Required configuration map. May contains:
    *   required "domainSet" - set of strings that represents the requested domain
    *
    * @return result map with keys:
    *   "STRING_IN_DOMAIN"
    */
  case class StringInDomainValuesMetricCalculator(cnt: Double,
                                                  paramMap: ParamMap)
      extends MetricCalculator {

    private val domain = paramMap("domain").asInstanceOf[Set[String]]

    def this(paramMap: Map[String, Any]) {
      this(0, paramMap)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      tryToString(values.head) match {
        case Some(v) =>
          if (domain.contains(v))
            StringInDomainValuesMetricCalculator(cnt + 1, paramMap)
          else this
        case None => this
      }
    }

    override def result(): Map[String, (Double, Option[String])] =
      Map(
        "STRING_IN_DOMAIN" + getParametrizedMetricTail(paramMap) -> (cnt, None))

    override def merge(m2: MetricCalculator): MetricCalculator =
      StringInDomainValuesMetricCalculator(
        this.cnt + m2.asInstanceOf[StringInDomainValuesMetricCalculator].cnt,
        paramMap)
  }

  /**
    * Calculates amount of string out of provided domain
    * @param cnt Current count of filtered elements
    * @param paramMap Required configuration map. May contains:
    *   required "domainSet" - set of strings that represents the requested domain
    *
    * @return result map with keys:
    *   "STRING_OUT_DOMAIN"
    */
  case class StringOutDomainValuesMetricCalculator(cnt: Double,
                                                   paramMap: ParamMap)
      extends MetricCalculator {

    private val domain: Set[String] = paramMap("domain").asInstanceOf[Set[String]]

    def this(paramMap: Map[String, Any]) {
      this(0, paramMap)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      tryToString(values.head) match {
        case Some(v) =>
          if (domain.contains(v)) this
          else StringOutDomainValuesMetricCalculator(cnt + 1, paramMap)
        case None => this
      }
    }

    override def result(): Map[String, (Double, Option[String])] =
      Map(
        "STRING_OUT_DOMAIN" + getParametrizedMetricTail(paramMap) -> (cnt, None))

    override def merge(m2: MetricCalculator): MetricCalculator =
      StringOutDomainValuesMetricCalculator(
        this.cnt + m2.asInstanceOf[StringOutDomainValuesMetricCalculator].cnt,
        paramMap)

  }

  /**
    * Calculates count of appearance of requested string in processed elements
    * @param cnt Current amount of appearances
    * @param paramMap Required configuration map. May contains:
    *   required "compareValue" - requested string to find
    *
    * @return result map with keys:
    *   "STRING_VALUES"
    */
  case class StringValuesMetricCalculator(cnt: Int, paramMap: ParamMap)
      extends MetricCalculator {

    private val lvalue: String = paramMap("compareValue").toString

    def this(paramMap: Map[String, Any]) {
      this(0, paramMap)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      StringValuesMetricCalculator(cnt + (if (values.head == lvalue) 1 else 0),
                                   paramMap)
    }

    override def result(): Map[String, (Double, Option[String])] =
      Map(
        "STRING_VALUES" + getParametrizedMetricTail(paramMap) -> (cnt.toDouble, None))

    override def merge(m2: MetricCalculator): MetricCalculator =
      StringValuesMetricCalculator(
        this.cnt + m2.asInstanceOf[StringValuesMetricCalculator].cnt,
        paramMap)

  }

}
