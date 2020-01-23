package dbmodel

import dbmodel.checks.Check
import dbmodel.metrics.Metric
import dbmodel.sources.{Database, Source}
import dbmodel.targets.Target
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.{KeyedEntity, Query, Table}

import scala.util.Try

trait SimpleService[T <: BasicEntity] {

  protected val mainTable: Table[T]

  val typeEnum: Option[Enumeration] = None
  val parentEnum: Option[EntityParentEnumeration] = None

  def getIdList(filter: Option[Enumeration#Value] = None): Query[String]
  def searchIdList(query: String, filter: Option[Enumeration#Value] = None, parent: Option[EntityParentEnumeration#EntityParentVal] = None, parentId: Option[String] = None): Query[String] = {
    (filter, parent, parentId) match {
      case (fil, Some(p), Some(pid)) => p.searchF(query, pid, fil)
      case (Some(fil),_,_) => from(mainTable)(t => where(t.id like query and t.getType === fil.toString) select t.id)
      case (_,_,_) => from(mainTable)(t => where(t.id like query) select t.id)
    }
  }

  def getAll(filter: Option[Enumeration#Value] = None): Query[T]
  def getDetailed(id: String): T = from(mainTable)(t => where(t.id === id) select t).single

  def deleteById(id: String): Int
  def updateReferenceById(old: String, nuovo: String): Unit

  def toShortMap(instance: T): Map[String, Any]

  def getStats(query: Iterable[T])(implicit pageLength: Option[Int] = None): Map[String, Int] = {
    val size: Int = query.size

    Map(
      "count" -> Some(size),
      "last_page" -> Try((size - 1)/pageLength.get).toOption
    ).filter(_._2.isDefined).map(t => t._1 -> t._2.get)
  }
}

trait BasicEntity extends KeyedEntity[String] {
  def insert(): BasicEntity
  def update(): BasicEntity
  def getType: String
}

trait EntityExtension extends KeyedEntity[String] {
  def insert(): EntityExtension
  def update(): EntityExtension
  def rebase(id: String): EntityExtension
}

trait EntityParentEnumeration extends Enumeration {
  case class EntityParentVal(name: String, searchF: (String, String, Option[Enumeration#Value]) => Query[String]) extends super.Val() {
    override def toString(): String = this.name
  }
  def convert(value: Value): EntityParentVal = value.asInstanceOf[EntityParentVal]
}

object BasicEntityType extends Enumeration {
  val database = BasicEntityVal("database", Database)
  val source = BasicEntityVal("source", Source)
  val metric = BasicEntityVal("metric", Metric)
  val check = BasicEntityVal("check", Check)
  val target = BasicEntityVal("target", Target)

  protected case class BasicEntityVal(name: String, service: SimpleService[_ <: BasicEntity]) extends super.Val() {
    override def toString(): String = this.name
  }
  implicit def convert(value: Value): BasicEntityVal = value.asInstanceOf[BasicEntityVal]
}