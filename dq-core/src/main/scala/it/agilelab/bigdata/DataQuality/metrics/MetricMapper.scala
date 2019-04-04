package it.agilelab.bigdata.DataQuality.metrics

import it.agilelab.bigdata.DataQuality.metrics.ColumnMetrics.AlgebirdMetrics.{HyperLogLogMetricCalculator, TopKMetricCalculator}
import it.agilelab.bigdata.DataQuality.metrics.ColumnMetrics.BasicNumericMetrics._
import it.agilelab.bigdata.DataQuality.metrics.ColumnMetrics.BasicStringMetrics._
import it.agilelab.bigdata.DataQuality.metrics.ColumnMetrics.MultiColumnMetrics.{CovarianceMetricCalculator, DayDistanceMetric, EqualStringColumnsMetricCalculator, LevenshteinDistanceMetric}

object MetricMapper extends Enumeration {

  val distinctValues = MetricDefinition("DISTINCT_VALUES", classOf[UniqueValuesMetricCalculator])
  val approxDistinctValues = MetricDefinition("APPROXIMATE_DISTINCT_VALUES", classOf[HyperLogLogMetricCalculator])
  val nullValues = MetricDefinition("NULL_VALUES", classOf[NullValuesMetricCalculator])
  val emptyValues = MetricDefinition("EMPTY_VALUES", classOf[EmptyStringValuesMetricCalculator])
  val minNumeric = MetricDefinition("MIN_NUMBER", classOf[MinNumericValueMetricCalculator])
  val maxNumeric = MetricDefinition("MAX_NUMBER", classOf[MaxNumericValueMetricCalculator])
  val sumOfValues = MetricDefinition("SUM_NUMBER", classOf[SumNumericValueMetricCalculator])
  val meanNumeric = MetricDefinition("AVG_NUMBER",classOf[StdAvgNumericValueCalculator])
  val stdDeviation = MetricDefinition("STD_NUMBER", classOf[StdAvgNumericValueCalculator])
  val minLength = MetricDefinition("MIN_STRING", classOf[MinStringValueMetricCalculator])
  val maxLength = MetricDefinition("MAX_STRING", classOf[MaxStringValueMetricCalculator])
  val meanLength = MetricDefinition("AVG_STRING", classOf[AvgStringValueMetricCalculator])
  val formattedDate = MetricDefinition("FORMATTED_DATE", classOf[DateFormattedValuesMetricCalculator])
  val formattedNumeric = MetricDefinition("FORMATTED_NUMBER", classOf[NumberFormattedValuesMetricCalculator])
  val formattedString = MetricDefinition("FORMATTED_STRING", classOf[StringFormattedValuesMetricCalculator])
  val castableNumeric = MetricDefinition("CASTED_NUMBER", classOf[NumberCastValuesMetricCalculator])
  val inDomainNumeric = MetricDefinition("NUMBER_IN_DOMAIN", classOf[NumberInDomainValuesMetricCalculator])
  val outOfDomainNumeric = MetricDefinition("NUMBER_OUT_DOMAIN", classOf[NumberOutDomainValuesMetricCalculator])
  val inDomainString = MetricDefinition("STRING_IN_DOMAIN", classOf[StringInDomainValuesMetricCalculator])
  val outOfDomainString = MetricDefinition("STRING_OUT_DOMAIN", classOf[StringOutDomainValuesMetricCalculator])
  val stringAppearance = MetricDefinition("STRING_VALUES", classOf[StringValuesMetricCalculator])
  val regexAppearance = MetricDefinition("REGEX_VALUES", classOf[RegexValuesMetricCalculator])
  val numberAppearance = MetricDefinition("NUMBER_VALUES", classOf[NumberValuesMetricCalculator])
  val medianValue = MetricDefinition("MEDIAN_VALUE", classOf[TDigestMetricCalculator])
  val firstQuantileValue = MetricDefinition("FIRST_QUANTILE", classOf[TDigestMetricCalculator])
  val thirdQuantileValue = MetricDefinition("THIRD_QUANTILE", classOf[TDigestMetricCalculator])
  val specificQuantile = MetricDefinition("GET_QUANTILE", classOf[TDigestMetricCalculator])
  val specificPercentile = MetricDefinition("GET_PERCENTILE", classOf[TDigestMetricCalculator])
  val topNRating = MetricDefinition("TOP_N", classOf[TopKMetricCalculator])
  val columnCompare = MetricDefinition("COLUMN_EQ", classOf[EqualStringColumnsMetricCalculator])
  val dayDistanceCompare = MetricDefinition("DAY_DISTANCE", classOf[DayDistanceMetric])
  val levenshteinDistanceCompare =  MetricDefinition("LEVENSHTEIN_DISTANCE", classOf[LevenshteinDistanceMetric])
  val comomentValue = MetricDefinition("CO-MOMENT", classOf[CovarianceMetricCalculator])
  val covarianceBiasedValue = MetricDefinition("COVARIANCE", classOf[CovarianceMetricCalculator])
  val covarianceValue = MetricDefinition("COVARIANCE_BESSEL", classOf[CovarianceMetricCalculator])

  def getMetricClass(name: String): Class[_ <: MetricCalculator with Product with Serializable] =
    convert(super.withName(name)).calculator

  protected case class MetricDefinition(
      name: String,
      calculator: Class[_ <: MetricCalculator with Product with Serializable])
      extends super.Val() {
    override def toString(): String = this.name
  }
  implicit def convert(value: Value): MetricDefinition =
    value.asInstanceOf[MetricDefinition]
}
