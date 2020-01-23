package dbmodel.results

import org.squeryl.annotations.Column

case class MetricResultsFileItemDB(
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
