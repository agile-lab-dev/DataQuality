package dbmodel.sources

import play.api.libs.json.{Format, Json}

case class MetricItem(
                       id: Option[String],
                       name: Option[String],
                       `type`: Option[String],
                       description: Option[String]
                     )

object MetricItem {
  implicit lazy val metricItemJsonFormat: Format[MetricItem] = Json.format[MetricItem]


}
