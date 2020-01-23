package com.agilelab.dataquality.api.model

import play.api.libs.json._

/**
  * Represents the Swagger definition for MetricsResultsResponse.
  */
@javax.annotation.Generated(value = Array("org.openapitools.codegen.languages.ScalaPlayFrameworkServerCodegen"), date = "2019-11-19T15:05:19.949+01:00[Europe/Rome]")
case class MetricsResultsResponse(
  totalItems: Option[Int],
  data: Option[List[MetricResultsItem]]
)

object MetricsResultsResponse {
  implicit lazy val metricsResultsResponseJsonFormat: Format[MetricsResultsResponse] = Json.format[MetricsResultsResponse]
}

