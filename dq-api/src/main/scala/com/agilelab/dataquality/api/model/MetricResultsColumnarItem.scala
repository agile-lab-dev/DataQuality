package com.agilelab.dataquality.api.model

import org.squeryl.annotations.Column
import play.api.libs.json.{Format, Json}


case class MetricResultsColumnarItem(
                                      @Column("metric_id")
                                      metricId: Option[String],
                                      @Column("source_date")
                                      date: Option[String],
                                      name: Option[String],
                                      @Column("source_id")
                                      sourceId: Option[String],
                                      @Column("column_names")
                                      columnNames: Option[String],
                                      params: Option[String],
                                      result: Option[String],
                                      @Column("additional_result")
                                      additionalResult: Option[String]
                                    )

object MetricResultsColumnarItem {
  implicit lazy val metricResultsColumnarItemJsonFormat: Format[MetricResultsColumnarItem] = Json.format[MetricResultsColumnarItem]
}



