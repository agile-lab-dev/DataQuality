package dbmodel.targets

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import dbmodel.AppDB
import dbmodel.targets.Target.TargetType.TargetType
import dbmodel.{ BasicEntity, EntityParentEnumeration, SimpleService}
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.annotations.Column
import org.squeryl.{Query, Table}

/**
  * Created by Egor Makhov on 08/08/2017.
  */

object Target extends SimpleService[Target]{

  protected val mainTable: Table[Target] = AppDB.targetTable

  object TargetType extends Enumeration {
    type TargetType = Value
    val basic = Value("BASIC")
    val system = Value("SYSTEM")
  }

  object ParentEnum extends EntityParentEnumeration {
    val check = EntityParentVal("check", getIdsByParentCheck)

    private def getIdsByParentCheck(query: String, parentId: String, filter: Option[Enumeration#Value] = None): Query[String] = {
      filter match {
        case _ =>
          from(AppDB.targetToChecksTable)(ttc => where(ttc.checkId === parentId and (ttc.targetId like query)) select ttc.targetId).distinct
      }
    }
  }

  override val typeEnum: Option[Enumeration] = Some(TargetType)
  override val parentEnum: Option[EntityParentEnumeration] = Some(ParentEnum)

  def applyOpt(idOpt: Option[String],
               targetType: String,
               fileFormat:String,
               path:String,
               delimiter:Option[String],
               savemode: Option[String],
               partitions: Option[Int],
               mails: List[String] = List.empty,
               checks: List[String] = List.empty
           ): (Target, List[Mail], List[TargetToChecks]) = {
    val id = idOpt.getOrElse(targetType)

    val mailList = mails.map(m => Mail(m, id))
    val checkList = checks.map(c => TargetToChecks(c, id))

    (
      Target(id,targetType,fileFormat,path,delimiter,savemode,partitions),
      mailList,
      checkList
    )
  }

  def unapplyOpt(bundle: (Target, List[Mail], List[TargetToChecks])): Option[(Option[String], String, String, String, Option[String], Option[String], Option[Int], List[String], List[String])] = {
    Option((
      Option(bundle._1.id),
      bundle._1.targetType,
      bundle._1.fileFormat,
      bundle._1.path,
      bundle._1.delimiter,
      bundle._1.savemode,
      bundle._1.partitions,
      bundle._2.map(m => m.address),
      bundle._3.map(c => c.checkId)
    ))
  }

  override def getIdList(filter: Option[Enumeration#Value] = None): Query[String] = {
    lazy val system: Query[String] = from(mainTable)(tbl => where(tbl.targetType === "SYSTEM")select tbl.id)
    lazy val basic: Query[String] = from(mainTable)(tbl => where(tbl.targetType <> "SYSTEM")select tbl.id)

    filter match {
      case Some(x: TargetType) if x == TargetType.basic  => basic
      case Some(x: TargetType) if x == TargetType.system => system
      case _ => from(mainTable)(tbl => select(tbl.id))
    }
  }

  override def getAll(filter: Option[Enumeration#Value] = None): Query[Target] = {
    lazy val system = from(mainTable)(tbl => where(tbl.targetType === "SYSTEM")select tbl)
    lazy val basic = from(mainTable)(tbl => where(tbl.targetType <> "SYSTEM")select tbl)

    filter match {
      case Some(x: TargetType) if x == TargetType.basic  => basic
      case Some(x: TargetType) if x == TargetType.system => system
      case _ => from(mainTable)(tbl => select(tbl))
    }
  }

  def getTargetListByCheck(check:String): Query[Target] = {
    from(mainTable)(tbl =>
      where(tbl.id in
        from(AppDB.targetToChecksTable)(tc =>
          where(tc.checkId === check)
          select tc.targetId
        )
      )
      select tbl)
  }

  def deleteAdditionsById(id: String): Int = {
    AppDB.targetToChecksTable.deleteWhere(ttc => ttc.targetId === id)
    AppDB.mailTable.deleteWhere(m => m.owner === id)
  }

  override def deleteById(id: String): Int = {
    deleteAdditionsById(id)
    mainTable.deleteWhere(tar => tar.id === id)
  }

  def deleteByCheck(check: String): Unit = {
    val sysTars: Query[Target] = getTargetListByCheck(check)
    sysTars.foreach(tar => deleteById(tar.id))
  }

  override def updateReferenceById(old: String, nuovo: String): Unit = {}

  override def toShortMap(instance: Target): Map[String, Any] = {
    Map("id"->instance.id, "targetType" -> instance.targetType)
  }

}

case class Target(
                   id: String,
                   @Column("target_type")
                   targetType: String,
                   @Column("file_format")
                   fileFormat:String,
                   path:String,
                   delimiter:Option[String],
                   savemode:Option[String],
                   partitions:Option[Int]
                 ) extends BasicEntity {

  // keep in mind, that it should be in transaction
  def insert(): Target = AppDB.targetTable.insert(this)
  def update(): Target = {
    AppDB.targetTable.update(this)
    this
  }

  @JsonIgnoreProperties(Array("owner"))
  lazy val mails: Iterator[Mail] =
    AppDB.targetToMails.left(this).iterator

//  @JsonIgnoreProperties(Array("owner"))
  lazy val checks: Iterator[TargetToChecks] =
    AppDB.targetToChecks.left(this).iterator

  override def getType: String = targetType
}