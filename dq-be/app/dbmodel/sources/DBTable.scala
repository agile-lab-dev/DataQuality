package dbmodel.sources

import dbmodel.AppDB
import dbmodel.ModelUtils._
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.annotations.Column
import org.squeryl.{KeyedEntity, Query}

/**
  * Created by Egor Makhov on 24/08/2017.
  */

object DBTable {

  def applyWithSource(
                     id:String,
                     keyFields: Seq[String],
                     database: String,
                     table: String,
                     username: Option[String],
                     password: Option[String]
                     ): (Source, DBTable) = {
    val kfString: Option[String] = toSeparatedString(keyFields)
    val src: Source = Source(id, "TABLE", kfString)
    val dbt: DBTable = DBTable(id,database,table,username,password)

    (src, dbt)
  }

  def unapplyWithSource(bundle: (Source, DBTable)): Option[(String, Seq[String], String, String, Option[String], Option[String])] = {
    val kfSeq: Seq[String] = parseSeparatedString(bundle._1.keyFields)
    Option((
      bundle._1.id,
      kfSeq,
      bundle._2.database,
      bundle._2.table,
      bundle._2.username,
      bundle._2.password
    ))
  }

  def getDetailed(id: String): (Source, DBTable) = {
    val src = from(AppDB.sourceTable) (dbt =>
      where(dbt.id === id)
        select dbt
    ).head
    val hdfs = from(AppDB.dbTableTable) (dbt =>
      where(dbt.id === id)
        select dbt
    ).head
    (src, hdfs)
  }

  def getByDatabase(db: String): Query[DBTable] = {
    from(AppDB.sourceTable, AppDB.dbTableTable) ((src, dbt) =>
      where(dbt.database === db and src.id === dbt.id)
        select dbt
    )
  }

  def deleteById(id: String): Int = {
    AppDB.dbTableTable.deleteWhere(db => db.id === id)
    Source.deleteById(id)
  }

  def updateReferenceById(old: String, nuovo: String): Unit = {
    Source.updateReferenceById(old, nuovo)
  }

  def fileToMap(source: (Source, DBTable)): Map[String, Any] = {
    val kfSeq: Seq[String] = source._1.keyFields match {
      case Some(kf) => kf.split(",").toSeq
      case None => Seq.empty
    }
    val res: Map[String, Option[Any]] = Map(
      "id"-> Option(source._1.id),
      "keyFields" -> Option(kfSeq),
      "database"-> Option(source._2.database),
      "table"-> Option(source._2.table),
      "username"-> source._2.username,
      "password"-> source._2.password
    )

    res.filter(_._2.isDefined).map(t => t._1 -> t._2.get)
  }

}

case class DBTable (
                    id: String,
                    database: String,
                    @Column("table_name")
                    table: String,
                    username: Option[String],
                    password: Option[String]
                  )
  extends KeyedEntity[String] {

//  private val service: DBTableService.type = DBTableService

  // keep in mind, that it should be in transaction
  def insert(): DBTable = {
    AppDB.dbTableTable.insert(this)
  }
  def update(): DBTable = {
    AppDB.dbTableTable.update(this)
    this
  }
  def rebase(id: String): DBTable = {
    this.insert()
    DBTable.updateReferenceById(id, this.id)
    DBTable.deleteById(id)
    this
  }

  // DATABASE =>
  def updateDatabase(db: String): DBTable = {
    val copy = this.copy(database = db)
    copy.update()
  }
}
