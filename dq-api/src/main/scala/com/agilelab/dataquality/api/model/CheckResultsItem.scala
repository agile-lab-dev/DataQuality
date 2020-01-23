package com.agilelab.dataquality.api.model

import play.api.libs.json._


/**
  * Represents the Swagger definition for CheckResultsItem.
  */
@javax.annotation.Generated(value = Array("org.openapitools.codegen.languages.ScalaPlayFrameworkServerCodegen"), date = "2019-11-19T15:05:19.949+01:00[Europe/Rome]")
case class CheckResultsItem(
  checkId: Option[String],
  checkName: Option[String],
  description: Option[String],
  checkedFile: Option[String],
  baseMetric: Option[String],
  comparedMetric: Option[String],
  comparedThreshold: Option[String],
  status: Option[String],
  message: Option[String],
  execDate: Option[String]
)

object CheckResultsItem {
  implicit lazy val checkResultsItemJsonFormat: Format[CheckResultsItem] = Json.format[CheckResultsItem]


  // noinspection TypeAnnotation
  object Status extends Enumeration {
    val Success = Value("Success")
    val Failure = Value("Failure")

    type Status = Value
    implicit lazy val StatusJsonFormat: Format[Value] = Format(Reads.enumNameReads(this), Writes.enumNameWrites[this.type])
  }

}
