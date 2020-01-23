package dbmodel.results

/**
  * Represents the Swagger definition for MetricResultsItem.
  */
@javax.annotation.Generated(value = Array("org.openapitools.codegen.languages.ScalaPlayFrameworkServerCodegen"),
  date = "2019-11-19T15:05:19.949+01:00[Europe/Rome]")
case class MetricResultsItemDB(
                                metricId: Option[String],
                                name: Option[String],
                                sourceId: Option[String],
                                result: Option[String],
                                metricType: Option[String],
                                date: Option[String]
                              )
