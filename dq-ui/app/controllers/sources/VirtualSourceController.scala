package controllers.sources

import javax.inject.Inject
import com.codahale.jerkson.Json.generate
import controllers.ControllerUtils._
import controllers.utils.MyDBSession
import controllers.utils.ValidationConstraints._
import models.sources._
import org.squeryl.PrimitiveTypeMode.inTransaction
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, seq, text}
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, Controller}

import scala.util.Try

/**
  * Created by Egor Makhov on 18/10/2017.
  */
class VirtualSourceController @Inject()(session: MyDBSession) extends Controller {

  private def getVSRCForm(currId: Option[String] = None): Form[(Source, VirtualSource)] = {
    def valFunc: (String) => Boolean = (t:String) => inTransaction(validateId(t, Source.getIdList(), currId))
    def valSrc: (String) => Boolean = (t:String) => inTransaction(validateRefList(t, Source.getIdList()))

    Form(
      mapping(
        "id" -> text.verifying(errorUsed, valFunc),
        "keyFields" -> seq(text),
        "tipo" -> text,
        "left" -> text.verifying(errorNotFound("Source"), valSrc),
        "right" -> optional(text),
        "query" -> text
      )(VirtualSource.applyWithSource)(VirtualSource.unapplyWithSource)
    )
  }

  def addVSRC() = Action { implicit request =>
    val form = getVSRCForm().bindFromRequest
    form.value map { file =>
      try {
        inTransaction{
          file._1.insert()
          file._2.insert()
          val json = generate(VirtualSource.fileToMap(file))
          Ok(json).as(JSON)
        }
      } catch {
        case e: Exception => InternalServerError(e.toString)
      }
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

  def getVSRCDetails(id: String) = Action {
    inTransaction{ Try{
      val file: (Source, VirtualSource) = VirtualSource.getDetailed(id)
      generate(VirtualSource.fileToMap(file))
    }.toOption match {
      case Some(json) => Ok(json).as(JSON)
      case None =>
        val json = generate(Map("error" -> "Virtual source not found!"))
        BadRequest(json).as(JSON)
    }}
  }

  def updateVSRC(id: String) = Action { implicit request =>
    val form = getVSRCForm(Some(id)).bindFromRequest
    form.value map { file =>
      inTransaction(try {
        file._1.id match {
          case `id` =>
            file._1.update()
            file._2.update()
          case _ =>
            file._1.insert()
            file._2.rebase(id)
        }
        val json = generate(VirtualSource.fileToMap(file))
        Ok(json).as(JSON)
      } catch {
        case e: Exception => InternalServerError(e.toString)
      })
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

}
