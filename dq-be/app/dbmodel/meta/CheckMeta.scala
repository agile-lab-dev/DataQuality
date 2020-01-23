package dbmodel.meta

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import dbmodel.AppDB
import dbmodel.checks.Check.CheckType.CheckType
import org.squeryl.PrimitiveTypeMode.{from, select, _}
import org.squeryl.Query
import org.squeryl.annotations.Column

/**
  * Created by Egor Makhov on 19/10/2017.
  */

object CheckMeta {
  def all(filter: Option[CheckType]): Query[CheckMeta] = {
    filter match {
      case Some(x:CheckType) =>
        from(AppDB.checkMetaTable)(tbl => where(tbl.checkType === x.toString.toLowerCase) select tbl )
      case _ =>
        from(AppDB.checkMetaTable)(tbl => select(tbl))
    }
  }

  def getShortList(filter: Option[CheckType]): Query[String] = {
    filter match {
      case Some(x:CheckType) =>
        from(AppDB.checkMetaTable)(tbl => where(tbl.checkType === x.toString.toLowerCase()) select tbl.id).distinct
      case _ =>
        from(AppDB.checkMetaTable)(tbl => select(tbl.id)).distinct
    }
  }

  def getById(id: String): Query[CheckMeta] = {
    from(AppDB.checkMetaTable)(tbl => where(tbl.id === id) select tbl)
  }

  def getTrendCheckRuleIds(): Query[String] = {
    from(AppDB.checkRuleTable)(r => select(r.name))
  }

  def getTrendCheckRules: Query[CheckRule] = {
    from(AppDB.checkRuleTable)(r => select(r))
  }


  def modeToString(withMetric: Boolean): String = if (withMetric) "with Metric" else "with Threshold"

}

@JsonIgnoreProperties(Array("check","withMetric"))
case class CheckParamMeta(
                            @Column("check_name")
                            check: String,
                            @Column("with_metric")
                            withMetric: Boolean,
                            name: String,
                            @Column("type")
                            paramType: String,
                            description: Option[String]
                          )

case class CheckRule(
                      name: String,
                      description: Option[String]
                    )

case class CheckMeta(
                       @Column("name")
                       id: String,
                       @Column("type")
                       checkType: String,
                       description: Option[String],
                       @Column("with_metric")
                       withMetric: Boolean
                     )
{
  lazy val parameters: Query[CheckParamMeta] = from(AppDB.checkParamMetaTable)(p =>
    where(p.check === this.id and p.withMetric === this.withMetric)
      select p)
}
