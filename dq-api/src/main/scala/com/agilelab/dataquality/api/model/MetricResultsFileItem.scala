package com.agilelab.dataquality.api.model

import org.squeryl.annotations.Column
import play.api.libs.json.{Format, Json}

case class MetricResultsFileItem(
                                  @Column("metric_id")
                                  metricId: Option[String],
                                  @Column("source_date")
                                  date: Option[String],
                                  name: Option[String],
                                  @Column("source_id")
                                  sourceId: Option[String],
                                  result: Option[String],
                                  @Column("additional_result")
                                  additionalResult: Option[String]
                            )

object MetricResultsFileItem {
  implicit lazy val metricResultsFileItemJsonFormat: Format[MetricResultsFileItem] = Json.format[MetricResultsFileItem]
}


