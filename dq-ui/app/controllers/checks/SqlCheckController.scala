package controllers.checks

import javax.inject.Inject
import com.codahale.jerkson.Json.generate
import controllers.ControllerUtils._
import controllers.utils.MyDBSession
import controllers.utils.ValidationConstraints._
import models.checks.Check.CheckType
import models.checks.{Check, SqlCheck}
import models.meta.CheckMeta
import models.sources.Database
import models.targets.TargetToChecks
import org.squeryl.PrimitiveTypeMode.inTransaction
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, Controller}

import scala.util.Try

/**
  * Created by Egor Makhov on 29/08/2017.
  */
class SqlCheckController @Inject()(session: MyDBSession) extends Controller {

  private def getMainForm(currId: Option[String] = None): Form[(Check, SqlCheck)] = {
    def valFunc: (String) => Boolean = (t:String) => inTransaction(validateId(t, Check.getIdList(), currId))
    def valDB: (String) => Boolean = (t:String) => inTransaction(validateRefList(t, Database.getIdList()))
    def valName: (String) => Boolean = (t:String) => inTransaction(validateRefList(t, CheckMeta.getShortList(Some(CheckType.sql))))

    Form(
      mapping(
        "id" -> text.verifying(errorUsed, valFunc),
        "subtype" -> text.verifying(errorNotFound("Check type"), valName),
        "description" -> optional(text),
        "database" -> text.verifying(errorNotFound("Database"), valDB),
        "query" -> text
      )(SqlCheck.applyWithCheck)(SqlCheck.unapplyWithCheck)
    )
  }

  def addSqlCheck() = Action { implicit request =>
    val form = getMainForm().bindFromRequest
    form.value map { check =>
      try {
        inTransaction({
          val chk = check._1.insert()
          val sql = check._2.insert()
          val json = generate(SqlCheck.tupleToMap((chk,sql)))
          Created(json).as(JSON)
        })
      } catch {
        case e: Exception => InternalServerError(e.toString)
      }
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

  def getCheckDetails(id: String) = Action {
    inTransaction(Try{
      val check: (Check, SqlCheck) = SqlCheck.getById(id)
      generate(SqlCheck.tupleToMap(check))
    }.toOption match {
      case Some(json) => Ok(json).as(JSON)
      case None =>
        val json = generate(Map("error"->"Check not found!"))
        BadRequest(json).as(JSON)
    })
  }

  def updateCheck(id: String) = Action { implicit request =>
    val form = getMainForm(Some(id)).bindFromRequest
    form.value map { check =>
      try {
          val json = inTransaction{
            if(check._1.id == id) {
              check._1.update()
              check._2.update()
            } else {
              val c = check._1.insert()
              check._2.insert()
              TargetToChecks.updateChecks(id, c.id)
              SqlCheck.deleteById(id)
            }
            generate(SqlCheck.tupleToMap((check._1,check._2)))
          }
          Ok(json).as(JSON)
        } catch {
        case e: Exception => InternalServerError(e.toString)
      }
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

}
