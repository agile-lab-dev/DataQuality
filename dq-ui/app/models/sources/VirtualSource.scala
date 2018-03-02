package models.sources

import models.AppDB
import models.ModelUtils._
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.annotations.Column

/**
  * Created by Egor Makhov on 18/10/2017.
  */
object VirtualSource {
  def applyWithSource(
                     id: String,
                     keyFields: Seq[String],
                     tipo: String,
                     left: String,
                     right: Option[String],
                     query: String
                     ): (Source, VirtualSource) = {
    val kfString: Option[String] = toSeparatedString(keyFields)
    val src: Source = Source(id, "VIRTUAL", kfString)
    val vsrc: VirtualSource = VirtualSource(id, tipo, left, right, query)
    (src, vsrc)
  }
  def unapplyWithSource(bundle: (Source, VirtualSource)): Option[(String, Seq[String], String, String, Option[String], String)] = {
    val kfSeq: Seq[String] = parseSeparatedString(bundle._1.keyFields)
    Option((
      bundle._1.id,
      kfSeq,
      bundle._2.tipo,
      bundle._2.left,
      bundle._2.right,
      bundle._2.query
    ))
  }

  def getDetailed(id: String): (Source, VirtualSource) = {
    val src = from(AppDB.sourceTable) (dbt =>
      where(dbt.id === id)
        select dbt
    ).head
    val vsrc = from(AppDB.virtualSourceTable) (dbt =>
      where(dbt.id === id)
        select dbt
    ).head
    (src, vsrc)
  }

  def deleteById(id: String): Int = {
    AppDB.virtualSourceTable.deleteWhere(db => db.id === id)
    Source.deleteById(id)
  }

  def updateReferenceById(old: String, nuovo: String): Unit = {
    Source.updateReferenceById(old, nuovo)
  }

  def fileToMap(source: (Source, VirtualSource)): Map[String, Any] = {
    val kfSeq: Seq[String] = source._1.keyFields match {
      case Some(kf) => kf.split(",").toSeq
      case None => Seq.empty
    }
    val res: Map[String, Option[Any]] = Map(
      "id" -> Option(source._1.id),
      "keyFields" -> Option(kfSeq),
      "tipo" -> Option(source._2.tipo),
      "left" -> Option(source._2.left),
      "right" -> source._2.right,
      "query" -> Option(source._2.query)
    )

    res.filter(_._2.isDefined).map(t => t._1 -> t._2.get)
  }
}

case class VirtualSource(
                        id: String,
                        tipo: String,
                        @Column("left_source")
                        left: String,
                        @Column("right_source")
                        right: Option[String] = None,
                        query: String
                        ) extends KeyedEntity[String] {
  def insert(): VirtualSource = {
    AppDB.virtualSourceTable.insert(this)
  }

  def update(): VirtualSource = {
    AppDB.virtualSourceTable.update(this)
    this
  }

  def rebase(id: String): VirtualSource = {
    this.insert()
    VirtualSource.updateReferenceById(id, this.id)
    VirtualSource.deleteById(id)
    this
  }
}
