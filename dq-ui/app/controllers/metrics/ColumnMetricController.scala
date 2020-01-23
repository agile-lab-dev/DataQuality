package controllers.metrics

import javax.inject.Inject
import com.codahale.jerkson.Json.generate
import controllers.ControllerUtils._
import controllers.utils.MyDBSession
import controllers.utils.ValidationConstraints._
import models.meta.MetricMeta
import models.metrics.Metric.MetricType
import models.metrics._
import models.sources.Source
import org.squeryl.PrimitiveTypeMode.inTransaction
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, seq, text, tuple}
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, Controller}

import scala.util.Try

/**
  * Created by Egor Makhov on 28/08/2017.
  */
class ColumnMetricController @Inject()(session: MyDBSession) extends Controller {

  private def getMainForm(currId: Option[String] = None): Form[(Metric, ColumnMetric, Seq[MetricParameter])] = {
    def valFunc: (String) => Boolean = (t:String) => inTransaction(validateId(t, Metric.getIdList(), currId))
    def valSrc: (String) => Boolean = (t:String) => inTransaction(validateRefList(t, Source.getIdList()))
    def valName: (String) => Boolean = (t:String) => inTransaction(validateRefList(t, MetricMeta.getShortList(Some(MetricType.column))))

    Form(
      mapping(
        "id" -> text.verifying(errorUsed, valFunc),
        "name" -> text.verifying(errorNotFound("Metric name"),valName),
        "description" -> text,
        "source" -> text.verifying(errorNotFound("Source"), valSrc),
        "columns" -> seq(nonEmptyText).verifying(nonEmptySeq),
        "parameters" -> seq(
          tuple(
            "name" -> text,
            "value" -> text
          )
        )
      )(ColumnMetric.applyWithMetric)(ColumnMetric.unapplyWithMetric).verifying(validateColumnMetric)
    )
  }

  def addColumnMetric() = Action { implicit request =>

    import play.api.Logger
    Logger.debug("TEST")

    val form = getMainForm().bindFromRequest
    form.value map { metric =>
      try {
        inTransaction({
          val met = metric._1.insert()
          val cmet = metric._2.insert()
          metric._3.foreach(_.insert())
          val json = generate(ColumnMetric.tupleToMap((met,cmet)))
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
        val metric: (Metric, ColumnMetric) = ColumnMetric.getById(id)
        generate(ColumnMetric.tupleToMap(metric))
      }.toOption match {
        case Some(json) => Ok(json).as(JSON)
        case None =>
          val json = generate(Map("error"->"Metric not found!"))
          BadRequest(json).as(JSON)
      })
  }

  def updateMetric(id: String) = Action { implicit request =>
    println(request)
    println(request.body)
    val form = getMainForm(Some(id)).bindFromRequest
    form.value map { metric =>
      inTransaction(try {
        MetricParameter.deleteByOwner(id)
        metric._1.id match {
          case `id` =>
            metric._1.update()
            metric._2.update()
          case _ =>
            metric._1.insert()
            metric._2.rebase(id)
        }
        metric._3.foreach(_.insert())
        val json = generate(ColumnMetric.tupleToMap(metric._1, metric._2))
        Ok(json).as(JSON)
      } catch {
        case e: Exception => InternalServerError(e.toString)
      })
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

}
