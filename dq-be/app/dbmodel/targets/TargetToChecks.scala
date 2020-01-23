package dbmodel.targets

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import dbmodel.AppDB
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.annotations.Column

/**
  * Created by Egor Makhov on 11/08/2017.
  */
object TargetToChecks {

  def applyWithoutOwner(check: String): TargetToChecks = {
    TargetToChecks(check)
  }
  def unapplyWithoutOwner(ttc: TargetToChecks): Option[String] = {
    Option(ttc.checkId)
  }

  def deleteByCheck(check: String): Int = {
    AppDB.targetToChecksTable.deleteWhere(m => m.checkId === check)
  }

  def deleteById(check: String, owner: String): Int = {
    AppDB.targetToChecksTable.deleteWhere(m => (m.checkId === check) and (m.targetId === owner))
  }

  def updateChecks(prev: String, curr: String): Int = {
    update(AppDB.targetToChecksTable)(ttc =>
      where(ttc.checkId === prev)
        set(ttc.checkId := curr)
    )
  }

  def getAvailableTargetsForCheck(check: String): Set[String] = {
    from(AppDB.targetTable)(tar =>
      where(tar.targetType === "SYSTEM" and (tar.id notIn from(AppDB.targetToChecksTable)(ttc =>
        where(ttc.checkId === check)
          select ttc.targetId)))
        select tar.id).toSet
  }

  def addTargetForCheck(check: String, list: Set[String]): Unit = {
    val ttcList: Set[TargetToChecks] = list map (x => TargetToChecks(check, x))
    AppDB.targetToChecksTable.insert(ttcList)
  }

}

@JsonIgnoreProperties(Array("targetId"))
case class TargetToChecks(
                           @Column("check_id")
                           checkId: String,
                           @Column("target_id")
                           targetId: String = ""
                         ) extends KeyedEntity[(String, String)] {
  override def id: (String, String) = (checkId, targetId)
  def insert: TargetToChecks = AppDB.targetToChecksTable.insert(this)
}
