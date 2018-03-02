package controllers.targets

import javax.inject.Inject

import com.codahale.jerkson.Json.generate
import models.targets.{Mail, Target, TargetToChecks}
import org.squeryl.PrimitiveTypeMode.inTransaction
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, Controller}

import scala.util.Try

/**
  * Created by Egor Makhov on 22/09/2017.
  */
class TargetAttachmentsController @Inject()() extends Controller {

  private val mailForm = Form(
    "address" -> email
  )

  def getTargetMailList(id: String) = Action {
    Try(inTransaction {
      val mails: Iterator[Mail] = Target.getDetailed(id).mails
      generate(mails)
    }).toOption match {
      case Some(json) => Ok(json).as(JSON)
      case None => BadRequest("There are no emails associated with this target!")
    }
  }

  def addTargetMail(id: String) = Action { implicit request =>
    val form = mailForm.bindFromRequest
    form.value map { mail =>
      inTransaction(Mail.apply(mail, id).insert)
      Created
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

  def deleteTargetMail(id: String) = Action { implicit request =>
    mailForm.bindFromRequest.value map (mail => {
      inTransaction(
        Mail.deleteById(mail, id)
      )
    })
    Ok
  }

  private val checkForm = Form(
    "checkId" -> text
  )

  def getTargetCheckList(id: String) = Action {
    Try(inTransaction {
      val checks: Iterator[TargetToChecks] = Target.getDetailed(id).checks
      generate(checks)
    }).toOption match {
      case Some(json) => Ok(json).as(JSON)
      case None => BadRequest("There are no checks associated with this target!")
    }
  }

  def addTargetCheck(id: String) = Action { implicit request =>
    checkForm.bindFromRequest.value map (check =>
      inTransaction(
        TargetToChecks.apply(check, id).insert
      )
      )
    Created
  }

  def deleteTargetCheck(id: String) = Action { implicit request =>
    checkForm.bindFromRequest.value map (check => {
      inTransaction(
        TargetToChecks.deleteById(check, id)
      )
    })
    Ok
  }

}
