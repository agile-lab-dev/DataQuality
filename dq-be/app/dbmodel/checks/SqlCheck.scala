package dbmodel.checks

import dbmodel.AppDB
import dbmodel.checks.Check.CheckType
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.{KeyedEntity, Query}

import scala.util.Try

/**
  * Created by Egor Makhov on 29/08/2017.
  */
object SqlCheck {

  def applyWithCheck(id: String, subtype:String, description: Option[String], database: String, query: String): (Check, SqlCheck) = {
    (new Check(id, CheckType.sql.toString, subtype, description), new SqlCheck(id, database, query))
  }

  def unapplyWithCheck(tuple: Check Tuple2 SqlCheck): Option[(String, String, Option[String], String, String)] = {
    Try((tuple._1.id, tuple._1.subtype, tuple._1.description, tuple._2.database, tuple._2.query)).toOption
  }

  def getById(id: String): (Check, SqlCheck) = {
    join(AppDB.sqlChecksTable, AppDB.checksTable) ((sc, check) =>
      where(sc.id === id)
        select (check, sc)
        on(sc.id === check.id)
    ).head
  }

  def getByDatabaseId(databaseId: String): Query[(Check, SqlCheck)] = {
    join(AppDB.sqlChecksTable, AppDB.checksTable) ((sc, check) =>
      where(sc.database === databaseId)
        select (check, sc)
        on(sc.id === check.id)
    )
  }

  def tupleToMap(bundle: Check Tuple2 SqlCheck): Map[String, Object] = {
    Map(
      "id"->bundle._1.id,
      "subtype"->bundle._1.subtype,
      "description"->bundle._1.description,
      "database"->bundle._2.database,
      "query"->bundle._2.query
    )
  }

  def deleteById(id: String): Int = {
    AppDB.sqlChecksTable.deleteWhere(met => met.id === id)
    Check.deleteById(id)
  }

}

case class SqlCheck (
                        id: String,
                        database: String,
                        query: String
                      )
  extends KeyedEntity[String] {
  // keep in mind, that it should be in transaction
  def insert(): SqlCheck = AppDB.sqlChecksTable.insert(this)
  def update(): SqlCheck = {
    AppDB.sqlChecksTable.update(this)
    this
  }
  def updateDatabase(db: String): SqlCheck = {
    val copy = this.copy(database = db)
    copy.update()
  }
}