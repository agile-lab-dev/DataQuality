package dbmodel.targets

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import dbmodel.AppDB
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._

/**
  * Created by Egor Makhov on 08/08/2017.
  */

object Mail {

  def applyWithoutOwner(mail: String): Mail = {
    Mail(mail)
  }
  def unapplyWithoutOwner(mail: Mail): Option[String] = {
    Option(mail.address)
  }

  def deleteById(mail: String, owner: String): Int = {
    AppDB.mailTable.deleteWhere(m => (m.address === mail) and (m.owner === owner))
  }
}

@JsonIgnoreProperties(Array("owner"))
case class Mail(
                 address: String,
                 owner: String = "") extends KeyedEntity[(String, String)] {
  override def id: (String, String) = (address, owner)
  def insert: Mail = AppDB.mailTable.insert(this)
}