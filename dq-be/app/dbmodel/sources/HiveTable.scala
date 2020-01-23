package dbmodel.sources

import dbmodel.AppDB
import dbmodel.ModelUtils._
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._

/**
  * Created by Egor Makhov on 24/08/2017.
  */
object HiveTable {

  def applyWithSource(
                       id:String,
                       keyFields: Seq[String],
                       date: String,
                       query: String
                     ): (Source, HiveTable) = {
    val kfString: Option[String] = toSeparatedString(keyFields)
    val src: Source = Source(id, "HIVE", kfString)
    val dbt: HiveTable = HiveTable(id,date,query)

    (src, dbt)
  }

  def unapplyWithSource(bundle: (Source, HiveTable)): Option[(String, Seq[String], String, String)] = {
    val kfSeq: Seq[String] = parseSeparatedString(bundle._1.keyFields)
    Option((
      bundle._1.id,
      kfSeq,
      bundle._2.date,
      bundle._2.query
    ))
  }

  def getDetailed(id: String): (Source, HiveTable) = {
    val src = from(AppDB.sourceTable) (dbt =>
      where(dbt.id === id)
        select dbt
    ).head
    val hdfs = from(AppDB.hiveTableTable) (dbt =>
      where(dbt.id === id)
        select dbt
    ).head
    (src, hdfs)
  }

  def deleteById(id: String): Int = {
    AppDB.hiveTableTable.deleteWhere(db => db.id === id)
    Source.deleteById(id)
  }

  def updateReferenceById(old: String, nuovo: String): Unit = {
    Source.updateReferenceById(old, nuovo)
  }

  def fileToMap(source: (Source, HiveTable)): Map[String, Any] = {
    val kfSeq: Seq[String] = source._1.keyFields match {
      case Some(kf) => kf.split(",").toSeq
      case None => Seq.empty
    }
    val res: Map[String, Option[Any]] = Map(
      "id"-> Option(source._1.id),
      "keyFields" -> Option(kfSeq),
      "date"-> Option(source._2.date),
      "query"-> Option(source._2.query)
    )

    res.filter(_._2.isDefined).map(t => t._1 -> t._2.get)
  }

}

case class HiveTable (
                     id: String,
                     date: String,
                     query: String
                   )
  extends KeyedEntity[String] {
  // keep in mind, that it should be in transaction

  def insert(): HiveTable = {
    AppDB.hiveTableTable.insert(this)
  }
  def update(): HiveTable = {
    AppDB.hiveTableTable.update(this)
    this
  }
  def rebase(id: String): HiveTable = {
    this.insert()
    HiveTable.updateReferenceById(id, this.id)
    HiveTable.deleteById(id)
    this
  }
}

