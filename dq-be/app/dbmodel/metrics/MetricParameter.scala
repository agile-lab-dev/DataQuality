package dbmodel.metrics

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import dbmodel.AppDB
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._

import scala.util.Try

/**
  * Created by Egor Makhov on 29/08/2017.
  */
object MetricParameter {
  def applyWithoutOwner(name: String, value: String): MetricParameter = {
    new MetricParameter("", name, value)
  }

  def unapplyWithoutOwner(arg: MetricParameter): Option[(String, String)] = {
    Try((arg.name, arg.value)).toOption
  }

  def deleteByName(name: String, owner: String): Int = {
    AppDB.metricParametersTable.deleteWhere(m => (m.name === name) and (m.owner === owner))
  }
  def deleteByOwner(owner: String): Int = {
    AppDB.metricParametersTable.deleteWhere(m => m.owner === owner)
  }
}

@JsonIgnoreProperties(Array("owner"))
case class MetricParameter(
                            owner: String,
                            name: String,
                            value: String
                          ) extends KeyedEntity[(String, String)] {

  override def id: (String, String) = (owner, name)
  def insert(): MetricParameter = AppDB.metricParametersTable.insert(this)
  def insertWithOwner(owner: String): MetricParameter = AppDB.metricParametersTable
    .insert(MetricParameter(owner,this.name,this.value))
}
