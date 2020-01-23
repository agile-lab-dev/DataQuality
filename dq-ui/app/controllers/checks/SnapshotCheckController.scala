package controllers.checks

import javax.inject.Inject
import com.codahale.jerkson.Json.generate
import controllers.ControllerUtils._
import controllers.utils.MyDBSession
import controllers.utils.ValidationConstraints._
import models.checks.Check.CheckType
import models.checks.{Check, CheckParameter, SnapshotCheck}
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
  * Created by Egor Makhov on 28/08/2017.
  */
class SnapshotCheckController @Inject()(session: MyDBSession) extends Controller {

  private def getMainForm(currId: Option[String] = None): Form[(Check, SnapshotCheck, List[CheckParameter])] = {
    def valFunc: (String) => Boolean = (t:String) => inTransaction(validateId(t, Check.getIdList(), currId))
    def valName: (String) => Boolean = (t:String) => inTransaction(validateRefList(t, CheckMeta.getShortList(Some(CheckType.snapshot))))
    def valMetric: (String) => Boolean = (t:String) => inTransaction(validateRefList(t, Metric.getIdList()))

    Form(
      mapping(
        "id" -> text.verifying(errorUsed, valFunc),
        "subtype" -> text.verifying(errorNotFound("Check type"), valName),
        "description" -> optional(text),
        "metric" -> text.verifying(errorNotFound("Metric"), valMetric),
        "parameters" -> list(
          tuple(
            "name" -> text,
            "value" -> text
          )
        )
      )(SnapshotCheck.applyWithCheck)(SnapshotCheck.unapplyWithCheck).verifying(validateSnapshotCheck)
    )
  }

  def addSnapshotCheck() = Action { implicit request =>
    val form = getMainForm().bindFromRequest
    form.value map { check =>
      try {
        val json = inTransaction({
          val met = check._1.insert()
          val colMet = check._2.insert()
          check._3.foreach(_.insert())
          generate(SnapshotCheck.tupleToMap((met,colMet)))
        })
        Created(json).as(JSON)
      } catch {
        case e: Exception => InternalServerError(e.toString)
      }
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

  def getCheckDetails(id: String) = Action {
    inTransaction { Try{
      val check: (Check, SnapshotCheck) = SnapshotCheck.getById(id).single
      generate(SnapshotCheck.tupleToMap(check))
    }.toOption match {
      case Some(json) => Ok(json).as(JSON)
      case None =>
        val json = generate(Map("error"->"Check not found!"))
        BadRequest(json).as(JSON)
      }
    }
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
            SnapshotCheck.deleteById(id)
          }
          check._3.foreach(_.insert())
          generate(SnapshotCheck.tupleToMap((check._1,check._2)))
        }
        Ok(json).as(JSON)
      } catch {
        case e: Exception => InternalServerError(e.toString)
      }
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

}
