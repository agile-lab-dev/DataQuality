package controllers.checks

import javax.inject.Inject
import com.codahale.jerkson.Json.generate
import controllers.ControllerUtils._
import controllers.utils.MyDBSession
import controllers.utils.ValidationConstraints._
import models.checks.Check.CheckType
import models.checks._
import models.meta.CheckMeta
import models.metrics.Metric
import models.targets.TargetToChecks
import org.squeryl.PrimitiveTypeMode.inTransaction
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms.{list, mapping, optional, text, tuple}
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, Controller}

import scala.util.Try

/**
  * Created by Egor Makhov on 29/08/2017.
  */
class TrendCheckController @Inject()(session: MyDBSession) extends Controller {

  private def getMainForm(currId: Option[String] = None): Form[(Check, TrendCheck, List[CheckParameter])] = {
    def valFunc: (String) => Boolean = (t:String) => inTransaction(validateId(t, Check.getIdList(), currId))
    def valName: (String) => Boolean = (t:String) => inTransaction(validateRefList(t, CheckMeta.getShortList(Some(CheckType.trend))))
    def valMetric: (String) => Boolean = (t:String) => inTransaction(validateRefList(t, Metric.getIdList()))
    def valRule: (String) => Boolean = (t:String) => inTransaction(validateRefList(t, CheckMeta.getTrendCheckRuleIds))

    Form(
      mapping(
        "id" -> text.verifying(errorUsed, valFunc),
        "subtype" -> text.verifying(errorNotFound("Check type"), valName),
        "description" -> optional(text),
        "metric" -> text.verifying(errorNotFound("Metric"), valMetric),
        "rule" -> text.verifying(errorNotFound("Provided rule"), valRule),
        "parameters" -> list(
          tuple(
            "name" -> text,
            "value" -> text
          )
        )
      )(TrendCheck.applyWithCheck)(TrendCheck.unapplyWithCheck).verifying(validateTrendCheck)
    )
  }

  def addTrendCheck() = Action { implicit request =>
    val form = getMainForm().bindFromRequest
    form.value map { check =>
      try {
        inTransaction({
          val chk = check._1.insert()
          val sql = check._2.insert()
          check._3.foreach(_.insert())
          val json = generate(TrendCheck.tupleToMap((chk,sql)))
          Created(json).as(JSON)
        })
      } catch {
        case e: Exception => InternalServerError(e.toString)
      }
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

  def getCheckDetails(id: String) = Action {
    inTransaction(Try{
      val check: (Check, TrendCheck) = TrendCheck.getById(id)
      generate(TrendCheck.tupleToMap(check))
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
            CheckParameter.deleteByOwner(id)
            check._1.update()
            check._2.update()
          } else {
            val c = check._1.insert()
            check._2.insert()
            TargetToChecks.updateChecks(id, c.id)
            TrendCheck.deleteById(id)
          }
          check._3.foreach(_.insert())
          generate(TrendCheck.tupleToMap((check._1,check._2)))
        }
        Ok(json).as(JSON)
      } catch {
        case e: Exception => InternalServerError(e.toString)
      }
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

}
