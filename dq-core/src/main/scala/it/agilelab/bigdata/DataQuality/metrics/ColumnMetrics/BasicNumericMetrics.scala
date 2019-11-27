package it.agilelab.bigdata.DataQuality.metrics.ColumnMetrics

import it.agilelab.bigdata.DataQuality.metrics.MetricCalculator
import it.agilelab.bigdata.DataQuality.metrics.MetricProcessor.ParamMap
import it.agilelab.bigdata.DataQuality.utils.{getParametrizedMetricTail, _}
import org.isarnproject.sketches.TDigest

import scala.math.sqrt
import scala.util.{Failure, Success, Try}

/**
  * Created by Egor Makhov on 29/05/2017.
  *
  * Basic metrics that can be applied to numerical elements
  */
object BasicNumericMetrics {

  /**
    * Calculates percentiles, quantiles for provided elements with use of TDigest library
    * https://github.com/isarn/isarn-sketches
    * @param tdigest Initial TDigest object
    * @param paramMap Required configuration map. May contains:
    *   "accuracy error" - required level of calculation accuracy. By default = 0.005
    *   "targetSideNumber" - required parameter
    *       For quantiles should be in [0,1]
    *
    * @return result map with keys:
    *   "GET_QUANTILE"
    *   "GET_PERCENTILE"
    *   "FIRST_QUANTILE"
    *   "THIRD_QUANTILE"
    *   "MEDIAN_VALUE"
    */
  case class TDigestMetricCalculator(tdigest: TDigest, paramMap: ParamMap)
      extends MetricCalculator {

    def this(paramMap: ParamMap) = {
      this(TDigest.empty(
             paramMap.getOrElse("accuracyError", 0.005).toString.toDouble),
           paramMap)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      tryToDouble(values.head) match {
        case Some(v) => TDigestMetricCalculator(tdigest.+(v), paramMap)
        case None    => this
      }
    }

    override def result(): Map[String, (Double, Option[String])] = {

      val requestedResult: Double =
        paramMap.getOrElse("targetSideNumber", 0).toString.toDouble

      val staticResults: Map[String, (Double, Option[String])] = Map(
        "MEDIAN_VALUE" + getParametrizedMetricTail(paramMap) -> (tdigest
          .cdfInverse(0.5), None),
        "FIRST_QUANTILE" + getParametrizedMetricTail(paramMap) -> (tdigest
          .cdfInverse(0.25), None),
        "THIRD_QUANTILE" + getParametrizedMetricTail(paramMap) -> (tdigest
          .cdfInverse(0.75), None)
      )

      // todo refactor after parameter grouping
      val parametrizedResults: Map[String, (Double, Option[String])] =
        requestedResult match {
          case x if x >= 0 && x <= 1 =>
            Map(
              "GET_QUANTILE" + getParametrizedMetricTail(paramMap) -> (tdigest
                .cdfInverse(x), None),
              "GET_PERCENTILE" + getParametrizedMetricTail(paramMap) -> (tdigest
                .cdf(x), None)
            )
          case x =>
            Map(
              "GET_PERCENTILE" + getParametrizedMetricTail(paramMap) -> (tdigest
                .cdf(x), None)
            )
        }

      staticResults ++ parametrizedResults
    }

    override def merge(m2: MetricCalculator): MetricCalculator =
      TDigestMetricCalculator(
        tdigest ++ m2.asInstanceOf[TDigestMetricCalculator].tdigest,
        paramMap)

  }

  /**
    * Calculates minimal value for provided elements
    * @param min Current minimal value
    *
    * @return result map with keys:
    *   "MIN_NUMBER"
    */
  case class MinNumericValueMetricCalculator(min: Double)
      extends MetricCalculator {

    def this(paramMap: Map[String, Any]) {
      this(Double.MaxValue)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      tryToDouble(values.head) match {
        case Some(v) => MinNumericValueMetricCalculator(Math.min(v, min))
        case None    => this
      }
    }

    override def result(): Map[String, (Double, Option[String])] =
      Map("MIN_NUMBER" -> (min.toDouble, None))

    override def merge(m2: MetricCalculator): MetricCalculator =
      MinNumericValueMetricCalculator(
        Math.min(this.min,
                 m2.asInstanceOf[MinNumericValueMetricCalculator].min))

  }

  /**
    * Calculates maximal value for provided elements
    * @param max Current maximal value
    *
    * @return result map with keys:
    *   "MAX_NUMBER"
    */
  case class MaxNumericValueMetricCalculator(max: Double)
      extends MetricCalculator {

    def this(paramMap: Map[String, Any]) {
      this(Double.MinValue)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      tryToDouble(values.head) match {
        case Some(v) => MaxNumericValueMetricCalculator(Math.max(v, max))
        case None    => this
      }
    }

    override def result(): Map[String, (Double, Option[String])] =
      Map("MAX_NUMBER" -> (max.toDouble, None))

    override def merge(m2: MetricCalculator): MetricCalculator =
      MaxNumericValueMetricCalculator(
        Math.max(this.max,
                 m2.asInstanceOf[MaxNumericValueMetricCalculator].max))

  }

  /**
    * Calculates sum of provided elements
    * @param sum Current sum
    *
    * @return result map with keys:
    *   "SUM_NUMBER"
    */
  case class SumNumericValueMetricCalculator(sum: Double)
      extends MetricCalculator {

    def this(paramMap: Map[String, Any]) {
      this(0)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      tryToDouble(values.head) match {
        case Some(v) => SumNumericValueMetricCalculator(v + sum)
        case None    => this
      }
    }

    override def result(): Map[String, (Double, Option[String])] =
      Map("SUM_NUMBER" -> (sum.toDouble, None))

    override def merge(m2: MetricCalculator): MetricCalculator =
      SumNumericValueMetricCalculator(
        this.sum + m2.asInstanceOf[SumNumericValueMetricCalculator].sum)

  }

  /**
    * Calculates standard deviation and mean value for provided elements
    * @param sum Current sum
    * @param sqSum Current squared sum
    * @param cnt Current element count
    *
    * @return result map with keys:
    *   "STD_NUMBER"
    *   "AVG_NUMBER"
    */
  case class StdAvgNumericValueCalculator(sum: Double, sqSum: Double, cnt: Int)
      extends MetricCalculator {

    def this(paramMap: Map[String, Any]) {
      this(0, 0, 0)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      tryToDouble(values.head) match {
        case Some(v) =>
          StdAvgNumericValueCalculator(sum + v, sqSum + (v * v), cnt + 1)
        case None => this
      }
    }

    override def result(): Map[String, (Double, Option[String])] = {
      val mean = sum / cnt.toDouble
      val variance = sqSum / cnt.toDouble - mean * mean
      Map(
        "STD_NUMBER" -> (sqrt(variance), None),
        "AVG_NUMBER" -> (mean, None)
      )
    }

    override def merge(m2: MetricCalculator): MetricCalculator = {
      val cm2 = m2.asInstanceOf[StdAvgNumericValueCalculator]
      StdAvgNumericValueCalculator(
        this.sum + cm2.sum,
        this.sqSum + cm2.sqSum,
        this.cnt + cm2.cnt
      )
    }
  }

  /**
    * Calculates amount of elements that fit provided format(precision and scale)
    * @param cnt Current amount of format fitted elements
    * @param paramMap Required configuration map. May contains:
    *   required "precision" - requested precision
    *   required "scale" - required scale
    *
    * @return result map with keys:
    *   "FORMATTED_NUMBER"
    */
  case class NumberFormattedValuesMetricCalculator(cnt: Double,
                                                   paramMap: ParamMap)
      extends MetricCalculator {

    private val precOpt: Option[Any] = paramMap.get("precision")
    private val scaleOpt: Option[Any] = paramMap.get("scale")

    def this(paramMap: Map[String, Any]) {
      this(0, paramMap)
    }

    val prec = precOpt match {
      case Some(p) => Option(p.toString)
      case None    => None
    }

    val scale = scaleOpt match {
      case Some(s) => Option(s.toString)
      case None    => None
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      val (typeErr, precErr, scaleErr) = checkNumber(values.head, prec, scale)
      NumberFormattedValuesMetricCalculator(
        cnt + (if (typeErr != 0 && precErr != 0 && scaleErr != 0) 1 else 0),
        paramMap)
    }

    override def result(): Map[String, (Double, Option[String])] =
      Map(
        "FORMATTED_NUMBER" + getParametrizedMetricTail(paramMap) -> (cnt, None))

    override def merge(m2: MetricCalculator): MetricCalculator =
      NumberFormattedValuesMetricCalculator(
        this.cnt + m2.asInstanceOf[NumberFormattedValuesMetricCalculator].cnt,
        paramMap)

    private def checkNumber(value: Any,
                            optPrec: Option[String],
                            optScale: Option[String]): (Int, Int, Int) = {
      Try {
        value.asInstanceOf[Double]
      } match {
        case Failure(e) => (0, 0, 0)
        case Success(e) => {
          val scaleError = optScale match {
            case None => 0
            case Some(scale) => {
              val split = value.asInstanceOf[String].split("\\.")
              if (split.length == 1) 0
              else if (split(0).length <= scale.toInt) 0
              else 1
            }
          }
          val precError = optPrec match {
            case None => 0
            case Some(prec) => {
              val strVal = value.asInstanceOf[String]
              val ll =
                if (strVal.contains(".")) strVal.length - 1 else strVal.length
              if (ll <= prec.toInt) 0 else 1
            }
          }
          (1, scaleError, precError)
        }
      }
    }
  }

  /**
    * Calculates amount of element that can be custed to numerical (double format)
    * @param cnt Current count of custabale elements
    *
    * @return result map with keys:
    *   "CASTED_NUMBER"
    */
  case class NumberCastValuesMetricCalculator(cnt: Double)
      extends MetricCalculator {

    def this(paramMap: Map[String, Any]) {
      this(0)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      if (Try { values.head.toString.toDouble }.isSuccess)
        NumberCastValuesMetricCalculator(cnt + 1)
      else this
    }

    override def result(): Map[String, (Double, Option[String])] =
      Map("CASTED_NUMBER" -> (cnt, None))

    override def merge(m2: MetricCalculator): MetricCalculator =
      NumberCastValuesMetricCalculator(
        this.cnt + m2.asInstanceOf[NumberCastValuesMetricCalculator].cnt)

  }

  /**
    * Calculated amount of elements in provided domain set
    * @param cnt Current count of elements in domain
    * @param paramMap Required configuration map. May contains:
    *   required "domainSet" - set of element that represent requested domain

    * @return result map with keys:
    *   "NUMBER_IN_DOMAIN"
    */
  case class NumberInDomainValuesMetricCalculator(cnt: Double,
                                                  paramMap: ParamMap)
      extends MetricCalculator {

    private val domain: Set[Double] = paramMap("domain").asInstanceOf[Set[String]].map(_.toDouble)

    def this(paramMap: Map[String, Any]) {
      this(0, paramMap)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      tryToDouble(values.head) match {
        case Some(v: Double) =>
          if (domain.contains(v))
            NumberInDomainValuesMetricCalculator(cnt + 1, paramMap)
          else this
        case None => this
      }
    }

    override def result(): Map[String, (Double, Option[String])] =
      Map(
        "NUMBER_IN_DOMAIN" + getParametrizedMetricTail(paramMap) -> (cnt, None))

    override def merge(m2: MetricCalculator): MetricCalculator =
      NumberInDomainValuesMetricCalculator(
        this.cnt + m2.asInstanceOf[NumberInDomainValuesMetricCalculator].cnt,
        paramMap)

  }

  /**
    * Calculates amount of elements out of provided domain set
    * @param cnt Current count of elements out of domain
    * @param paramMap Required configuration map. May contains:
    *   required "domainSet" - set of element that represent requested domain

    * @return result map with keys:
    *   "NUMBER_OUT_DOMAIN"
    */
  case class NumberOutDomainValuesMetricCalculator(cnt: Double,
                                                   paramMap: ParamMap)
      extends MetricCalculator {

    private val domain = paramMap("domain").asInstanceOf[Set[Double]]

    def this(paramMap: Map[String, Any]) {
      this(0, paramMap)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      tryToDouble(values.head) match {
        case Some(v) =>
          if (domain.contains(v)) this
          else NumberOutDomainValuesMetricCalculator(cnt + 1, paramMap)
        case None => this
      }
    }

    override def result(): Map[String, (Double, Option[String])] =
      Map(
        "NUMBER_OUT_DOMAIN" + getParametrizedMetricTail(paramMap) -> (cnt, None))

    override def merge(m2: MetricCalculator): MetricCalculator =
      NumberOutDomainValuesMetricCalculator(
        this.cnt + m2.asInstanceOf[NumberOutDomainValuesMetricCalculator].cnt,
        paramMap)

  }

  /**
    * Calculates count of requested value's appearance in processed elements
    * @param cnt Current count of appearance
    * @param paramMap Required configuration map. May contains:
    *   required "compareValue" - target value to track

    * @return result map with keys:
    *   "NUMBER_VALUES"
    */
  case class NumberValuesMetricCalculator(cnt: Int, paramMap: ParamMap)
      extends MetricCalculator {

    private val lvalue: Double = paramMap("compareValue").toString.toDouble

    def this(paramMap: Map[String, Any]) {
      this(0, paramMap)
    }

    override def increment(values: Seq[Any]): MetricCalculator = {
      tryToDouble(values.head) match {
        case Some(v) =>
          NumberValuesMetricCalculator(cnt + (if (v == lvalue) 1 else 0),
                                       paramMap)
        case None => this
      }
    }

    override def result(): Map[String, (Double, Option[String])] =
      Map(
        "NUMBER_VALUES" + getParametrizedMetricTail(paramMap) -> (cnt.toDouble, None))

    override def merge(m2: MetricCalculator): MetricCalculator =
      NumberValuesMetricCalculator(
        this.cnt + m2.asInstanceOf[NumberValuesMetricCalculator].cnt,
        paramMap)
  }

}
