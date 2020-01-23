package dbmodel.sources

import dbmodel.AppDB
import dbmodel.checks.SqlCheck
import dbmodel.{ BasicEntity, SimpleService}
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.annotations.Column
import org.squeryl.{Query, Table}

/**
  * Created by Egor Makhov on 17/08/2017.
  */

object Database extends SimpleService[Database] {




  protected val mainTable: Table[Database] = AppDB.databaseTable

  override def getIdList(filter: Option[Enumeration#Value] = None): Query[String] = {
    from(mainTable)(t => select(t.id))
  }

  override def getAll(filter: Option[Enumeration#Value] = None): Query[Database] = {
    from(mainTable)(tbl => select(tbl))
  }

  def getBySources(sources: List[String]): Query[Database] = {
    from(mainTable)(db=> where( db.id in sources ) select(db) )
  }

  override def deleteById(id: String): Int = {
    SqlCheck.getByDatabaseId(id) foreach(bundle => SqlCheck.deleteById(bundle._1.id))
    DBTable.getByDatabase(id) foreach(table => DBTable.deleteById(table.id))

    mainTable.deleteWhere(db => db.id === id)
  }

  override def updateReferenceById(old: String, nuovo: String): Unit = {
    SqlCheck.getByDatabaseId(old) foreach (bundle => bundle._2.updateDatabase(nuovo))
    DBTable.getByDatabase(old) foreach (table => table.updateDatabase(nuovo))
  }

  override def toShortMap(instance: Database): Map[String, Any] = {
    Map("id" -> instance.id, "host" -> instance.host)
  }
}

case class Database (
                      id: String,
                      subtype: String,
                      host: String,
                      port: Option[Int],
                      service: Option[String],
                      @Column("username")
                      user: Option[String],
                      password: Option[String])
  extends BasicEntity {

  override def getType: String = subtype
  // keep in mind, that it should be in transaction
  override def insert(): Database = AppDB.databaseTable.insert(this)
  override def update(): Database = {
    AppDB.databaseTable.update(this)
    this
  }
  def rebase(id: String): Database = {
    this.insert()
    Database.updateReferenceById(id, this.id)
    Database.deleteById(id)
    this
  }
}
