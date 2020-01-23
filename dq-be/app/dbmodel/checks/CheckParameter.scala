package dbmodel.checks

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import dbmodel.AppDB
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._

import scala.util.Try

/**
  * Created by Egor Makhov on 29/08/2017.
  */
object CheckParameter {
  def applyWithoutOwner(name: String, value: String): CheckParameter = {
    new CheckParameter("", name, value)
  }

  def unapplyWithoutOwner(arg: CheckParameter): Option[(String, String)] = {
    Try((arg.name, arg.value)).toOption
  }

  def deleteByName(name: String, owner: String): Int = {
    AppDB.checkParametersTable.deleteWhere(m => (m.name === name) and (m.owner === owner))
  }
  def deleteByOwner(owner: String): Int = {
    AppDB.checkParametersTable.deleteWhere(m => m.owner === owner)
  }
}

@JsonIgnoreProperties(Array("owner"))
case class CheckParameter(
                            owner: String,
                            name: String,
                            value: String
                          ) extends KeyedEntity[(String, String)] {

  override def id: (String, String) = (owner, name)
  def insert(): CheckParameter = AppDB.checkParametersTable.insert(this)
  def insertWithOwner(owner: String): CheckParameter = AppDB.checkParametersTable
    .insert(CheckParameter(owner,this.name,this.value))
}
