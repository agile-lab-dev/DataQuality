package com.agilelab.dataquality.api.model

import com.agilelab.dataquality.api.model.SourceItem
import play.api.libs.json._

/**
  * Represents the Swagger definition for SourcesResultsResponse.
  */
@javax.annotation.Generated(value = Array("org.openapitools.codegen.languages.ScalaPlayFrameworkServerCodegen"), date = "2019-11-19T15:05:19.949+01:00[Europe/Rome]")
case class SourcesResultsResponse(
  totalItems: Option[Int],
  data: Option[List[SourceItem]]
)

object SourcesResultsResponse {
  implicit lazy val sourcesResultsResponseJsonFormat: Format[SourcesResultsResponse] = Json.format[SourcesResultsResponse]
}

