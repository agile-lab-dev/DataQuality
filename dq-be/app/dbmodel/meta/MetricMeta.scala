package dbmodel.meta

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import dbmodel.AppDB
import dbmodel.metrics.Metric.MetricType.MetricType
import org.squeryl.PrimitiveTypeMode.{from, select, _}
import org.squeryl.annotations.Column
import org.squeryl.{KeyedEntity, Query}

/**
  * Created by Egor Makhov on 19/10/2017.
  */

object MetricMeta {

  def all(filter: Option[MetricType]): Query[MetricMeta] = {
    filter match {
      case Some(x:MetricType) =>
        from(AppDB.metricMetaTable)(tbl => where(tbl.metricType === x.toString) select tbl)
      case _ =>
        from(AppDB.metricMetaTable)(tbl => select(tbl))
    }
  }

  def getShortList(filter: Option[MetricType]): Query[String] = {
    filter match {
      case Some(x:MetricType) =>
        from(AppDB.metricMetaTable)(tbl => where(tbl.metricType === x.toString) select tbl.id).distinct
      case _ =>
        from(AppDB.metricMetaTable)(tbl => select(tbl.id)).distinct
    }
  }

  def getById(id: String): Query[MetricMeta] = {
    from(AppDB.metricMetaTable)(tbl => where(tbl.id === id) select tbl)
  }

  def getParamsById(id: String): Query[MetricParamMeta] = {
    from(AppDB.metricParamMetaTable)(tbl => where(tbl.metric === id) select tbl)
  }
}

@JsonIgnoreProperties(Array("metric"))
case class MetricParamMeta(
                          metric: String,
                          name: String,
                          @Column("type")
                          paramType: String,
                          description: Option[String],
                          @Column("is_optional")
                          isOptional: Boolean
                          ) extends KeyedEntity[(String, String)] {
  override def id: (String, String) = (metric, name)
}

case class MetricMeta(
                     @Column("name")
                     id: String,
                     @Column("type")
                     metricType: String,
                     description: Option[String],
                     @Column("is_statusable")
                     isStatusable: Boolean,
                     @Column("is_multicolumn")
                     isMulticolumn: Boolean
                      ) extends KeyedEntity[String]  {

  lazy val parameters: Iterator[MetricParamMeta] =
    AppDB.metaMetricToParameters.left(this).iterator
}
