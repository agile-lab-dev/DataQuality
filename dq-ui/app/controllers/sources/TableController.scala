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
  * Created by Egor Makhov on 24/08/2017.
  */
class TableController @Inject()(session: MyDBSession) extends Controller {

  private def getDBTForm(currId: Option[String] = None): Form[(Source, DBTable)] = {
    def valFunc: (String) => Boolean = (t:String) => inTransaction(validateId(t, Source.getIdList(), currId))
    def valDb: (String) => Boolean = (t:String) => inTransaction(validateRefList(t, Database.getIdList()))

    Form(
      mapping(
        "id" -> text.verifying(errorUsed, valFunc),
        "keyFields" -> seq(text),
        "database" -> text.verifying(errorNotFound("Database"), valDb),
        "table" -> text,
        "username" -> optional(text),
        "password" -> optional(text)
      )(DBTable.applyWithSource)(DBTable.unapplyWithSource)
    )
  }

  def addDBTable() = Action { implicit request =>
    val form = getDBTForm().bindFromRequest
    form.value map { table =>
      try {
        inTransaction {
          val src = table._1.insert()
          val dbt = table._2.insert()
          val json = generate(DBTable.fileToMap(src,dbt))
          Created(json).as(JSON)
        }
      } catch {
        case e: Exception => InternalServerError(e.toString)
      }
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

  def getDBTableDetails(id: String) = Action {
    inTransaction(Try{
      val dbt: (Source, DBTable) = DBTable.getDetailed(id)
      generate(DBTable.fileToMap(dbt))
    }.toOption match {
      case Some(json) => Ok(json).as(JSON)
      case None =>
        val json = generate(Map("error"->"Table not found!"))
        BadRequest(json).as(JSON)
    })
  }

  def updateDBTable(id: String) = Action { implicit request =>
    val form = getDBTForm(Some(id)).bindFromRequest
    form.value map { dbt =>
      try{ inTransaction{
        dbt._1.id match {
          case `id` =>
            dbt._1.update()
            dbt._2.update()
          case _ =>
            dbt._1.insert()
            dbt._2.rebase(id)
        }
        val json = generate(DBTable.fileToMap(dbt))
        Ok(json).as(JSON)
      }} catch {
        case e: Exception => InternalServerError(e.toString)
      }
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

  private def getHTForm(currId: Option[String] = None): Form[(Source, HiveTable)] = {
    def valFunc: (String) => Boolean = (t:String) => inTransaction(validateId(t, Source.getIdList(), currId))
    //    .verifying(errorUsed, valFunc)

    Form(
      mapping(
        "id" -> text.verifying(errorUsed, valFunc),
        "keyFields" -> seq(text),
        "date" -> text,
        "query" -> text
      )(HiveTable.applyWithSource)(HiveTable.unapplyWithSource)
    )
  }


  def addHiveTable() = Action { implicit request =>
    val form = getHTForm().bindFromRequest
    form.value map { table =>
      try {
        inTransaction {
          table._1.insert()
          table._2.insert()
          val json = generate(HiveTable.fileToMap(table))
          Ok(json).as(JSON)
        }
      } catch {
        case e: Exception => InternalServerError(e.toString)
      }
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

  def getHiveTableDetails(id: String) = Action {
    inTransaction(Try{
      val ht: (Source, HiveTable) = HiveTable.getDetailed(id)
      generate(HiveTable.fileToMap(ht))
    }.toOption match {
      case Some(json) => Ok(json).as(JSON)
      case None =>
        val json = generate(Map("error"->"Table not found!"))
        BadRequest(json).as(JSON)
    })
  }

  def updateHiveTable(id: String) = Action { implicit request =>
    val form = getHTForm(Some(id)).bindFromRequest
    form.value map { hvt =>
      try{ inTransaction{
        hvt._1.id match {
          case `id` =>
            hvt._1.update()
            hvt._2.update()
          case _ =>
            hvt._1.insert()
            hvt._2.rebase(id)
        }
        val json = generate(HiveTable.fileToMap(hvt))
        Ok(json).as(JSON)
      }} catch {
        case e: Exception => InternalServerError(e.toString)
      }
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

}
