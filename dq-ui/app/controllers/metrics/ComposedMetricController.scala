package controllers.metrics

import javax.inject.Inject
import com.codahale.jerkson.Json.generate
import controllers.ControllerUtils._
import controllers.utils.MyDBSession
import controllers.utils.ValidationConstraints._
import models.metrics._
import org.squeryl.PrimitiveTypeMode.inTransaction
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms.{mapping, text}
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, Controller}

import scala.util.Try

/**
  * Created by Egor Makhov on 28/08/2017.
  */
class ComposedMetricController @Inject()(session: MyDBSession) extends Controller {

  private def getMainForm(currId: Option[String] = None): Form[(Metric, ComposedMetric)] = {
    def validateIdFunc: (String) => Boolean = (t:String) => inTransaction(validateId(t, Metric.getIdList(), currId))
    def validateFormulaFunc: (String) => Boolean = (t:String) => inTransaction(ComposedMetricValidator.validateFormula(t, currId))

    Form(
      mapping(
        "id" -> text.verifying(errorUsed, validateIdFunc),
        "name" -> text,
        "description" -> text,
        "formula" -> text.verifying(errorFormula, validateFormulaFunc)
      )(ComposedMetric.applyWithMetric)(ComposedMetric.unapplyWithMetric)
    )
  }

  def addComposedMetric() = Action { implicit request =>
    val form = getMainForm().bindFromRequest
    form.value map { metric =>
      try {
        inTransaction({
          val met = metric._1.insert()
          val fmet = metric._2.insert()
          val json = generate(ComposedMetric.tupleToMap((met,fmet)))
          Created(json).as(JSON)
        })
      } catch {
        case e: Exception => InternalServerError(e.toString)
      }
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

  def getMetricDetails(id: String) = Action {
    inTransaction(
      Try{
        val metric: (Metric, ComposedMetric) = ComposedMetric.getById(id)
        generate(ComposedMetric.tupleToMap(metric))
      }.toOption match {
        case Some(json) => Ok(json).as(JSON)
        case None =>
          val json = generate(Map("error"->"Metric not found!"))
          BadRequest(json).as(JSON)
      })
  }

  def updateMetric(id: String) = Action { implicit request =>
    val form = getMainForm(Some(id)).bindFromRequest
    form.value map { metric =>
      inTransaction(try {
        metric._1.id match {
          case `id` =>
            metric._1.update()
            metric._2.update()
          case _ =>
            metric._1.insert()
            metric._2.rebase(id)
        }
        val json = generate(ComposedMetric.tupleToMap(metric))
        Ok(json).as(JSON)
      } catch {
        case e: Exception => InternalServerError(e.toString)
      })
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

}
