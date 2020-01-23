package com.agilelab.dataquality.api.model

import play.api.libs.json._

/**
  * Represents the Swagger definition for ChecksResultsResponse.
  */
@javax.annotation.Generated(value = Array("org.openapitools.codegen.languages.ScalaPlayFrameworkServerCodegen"), date = "2019-11-19T15:05:19.949+01:00[Europe/Rome]")
case class ChecksResultsResponse(
  totalItems: Option[Int],
  data: Option[Seq[CheckResultsItem]]
)

object ChecksResultsResponse {
  implicit lazy val checksResultsResponseJsonFormat: Format[ChecksResultsResponse] = Json.format[ChecksResultsResponse]
}

