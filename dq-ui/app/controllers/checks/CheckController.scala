package controllers.checks

import javax.inject.Inject
import com.codahale.jerkson.Json.generate
import controllers.utils.MyDBSession
import controllers.utils.ResultWrappers._
import models.checks.Check.CheckType
import models.checks.{Check, SnapshotCheck, SqlCheck, TrendCheck}
import models.targets.TargetToChecks
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import play.api.Configuration
import play.api.Play.current
import play.api.data.Forms._
import play.api.data.{Form, Forms}
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, Controller}

import scala.util.Try

/**
  * Created by Egor Makhov on 29/08/2017.
  */
class CheckController @Inject()(val configuration: Configuration,session: MyDBSession) extends Controller {

  private implicit val pageLength: Option[Int] = configuration.getInt("pagination.length")

  def getAllChecks(database: Option[String], metric: Option[String], page: Option[Int], filter: Option[String]) = Action {
    safeResultInTransaction {
      val chkType = Try(CheckType.withName(filter.get.toUpperCase)).toOption
      val json: String = {
        val query: Query[Check] = (database, metric) match {
          case (Some(db), None) => Check.getChecksByDatabase(db)
          case (None, Some(met)) => Check.getChecksByMetric(met, chkType)
          case _ => Check.getAll(chkType)
        }
        val stats = Check.getStats(query)
        val res = (pageLength,page,query) match {
          case (Some(length), Some(pg), qr: Query[Check]) => qr.page(pg * length, length)
          case (_,_,_) => query
        }
        generate(Map("checks" -> res.map(Check.toShortMap)) ++ stats)
      }
      Ok(json).as(JSON)
    }
  }

  def getIdList(filter: Option[String]) = Action {
    val chkType = Try(CheckType.withName(filter.get.toUpperCase)).toOption
    val json = inTransaction {
      val list: Query[String] = Check.getIdList(chkType)
      generate(Map("checks" -> list.iterator))
    }
    Ok(json).as(JSON)
  }

  def deleteCheckById(id: String) = Action {
    safeResultInTransaction {
      val delOpt: Option[Int] = Try {
        val src: String = Check.getDetailed(id).cType
        src.toUpperCase match {
          case "SQL" => SqlCheck.deleteById(id)
          case "SNAPSHOT" => SnapshotCheck.deleteById(id)
          case "TREND" => TrendCheck.deleteById(id)
          case _ => 0
        }
      }.toOption
      delOpt match {
        case Some(x) if x > 0 => Ok
        case _ =>
          val json = generate(Map("error"->"Check not found!"))
          BadRequest(json).as(JSON)
      }
    }
  }

  // routes TO
  private val targetForm: Form[Set[String]] = Form(
    single("targets" -> Forms.set(single("id" -> text)))
  )

  def listAvailableTargets(id: String) = Action {
    val json = inTransaction{
      val targetList: Set[String] = TargetToChecks.getAvailableTargetsForCheck(id)
      generate(Map("targets"->targetList.map(t => Map("id"-> t))))
    }
    Ok(json).as(JSON)
  }

  def addCheckToTargets(id: String) = Action { implicit request =>
    val form = targetForm.bindFromRequest
    form.value map { list => inTransaction{
      val controlList: Set[String] = TargetToChecks.getAvailableTargetsForCheck(id)
      if (controlList.intersect(list) == list) {
        TargetToChecks.addTargetForCheck(id, list)
        Ok
      } else {
        val withErrors = form
          .withError("targets", "Target list is not valid!")
        BadRequest(withErrors.errorsAsJson).as(JSON)
      }
    }} getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }
}

