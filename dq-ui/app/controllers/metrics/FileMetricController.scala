package controllers.metrics

import javax.inject.Inject
import com.codahale.jerkson.Json.generate
import controllers.ControllerUtils._
import controllers.utils.MyDBSession
import controllers.utils.ValidationConstraints._
import models.meta.MetricMeta
import models.metrics.Metric.MetricType
import models.metrics.{FileMetric, Metric}
import models.sources.Source
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
class FileMetricController @Inject()(session: MyDBSession) extends Controller {

  private def getMainForm(currId: Option[String] = None): Form[(Metric, FileMetric)] = {
    def valFunc: (String) => Boolean = (t:String) => inTransaction(validateId(t, Metric.getIdList(), currId))
    def valSrc: (String) => Boolean = (t:String) => inTransaction(validateRefList(t, Source.getIdList()))
    def valName: (String) => Boolean = (t:String) => inTransaction(validateRefList(t, MetricMeta.getShortList(Some(MetricType.file))))

    Form(
      mapping(
        "id" -> text.verifying(errorUsed, valFunc),
        "name" -> text.verifying(errorNotFound("Metric name"), valName),
        "description" -> text,
        "source" -> text.verifying(errorNotFound("Source"), valSrc)
      )(FileMetric.applyWithMetric)(FileMetric.unapplyWithMetric).verifying(validateFileMetric)
    )
  }

  def addFileMetric() = Action { implicit request =>
    val form = getMainForm().bindFromRequest
    form.value map { metric =>
      try {
        inTransaction({
          val met = metric._1.insert()
          val fmet = metric._2.insert()
          val json = generate(FileMetric.tupleToMap((met,fmet)))
          Created(json).as(JSON)
        })
      } catch {
        case e:Exception => InternalServerError(e.toString)
      }
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

  def getMetricDetails(id: String) = Action {
    inTransaction(
      Try{
        val metric: (Metric, FileMetric) = FileMetric.getById(id)
        generate(FileMetric.tupleToMap(metric))
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
        val json = generate(FileMetric.tupleToMap(metric))
        Ok(json).as(JSON)
      } catch {
        case e: Exception => InternalServerError(e.toString)
      })
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

}
