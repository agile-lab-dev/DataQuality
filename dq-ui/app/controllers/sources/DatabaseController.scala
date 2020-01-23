package controllers.sources

import javax.inject.Inject
import com.codahale.jerkson.Json.generate
import controllers.ControllerUtils._
import controllers.utils.MyDBSession
import controllers.utils.ValidationConstraints._
import models.sources.Database
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import play.api.Configuration
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, Controller}

import scala.util.Try

/**
  * Created by Egor Makhov on 24/08/2017.
  */
class DatabaseController @Inject()(val configuration: Configuration,session: MyDBSession) extends Controller {

  private implicit val pageLength: Option[Int] = configuration.getInt("pagination.length")

  private def getMainForm(currId: Option[String] = None): Form[Database] = {
    def valFunc: (String) => Boolean = (t:String) => inTransaction(validateId(t, Database.getIdList(), currId))

    Form(
      mapping(
        "id" -> text.verifying(errorUsed, valFunc),
        "subtype" -> text,
        "host" -> text,
        "port" -> optional(number),
        "service" -> optional(text),
        "username" -> optional(text),
        "password" -> optional(text)
      )(Database.apply)(Database.unapply)
    )
  }

  def getAllDatabases(page: Option[Int]) = Action {
    val json: String = inTransaction {
      val query: Query[Database] = Database.getAll()
      val stats: Map[String, Int] = Database.getStats(query)
      val res = (pageLength,page) match {
        case (Some(length), Some(pg)) => query.page(pg * length, length)
        case (_,_) => query
      }
      generate(Map("databases" -> res.map(Database.toShortMap)) ++ stats)
    }
    Ok(json).as(JSON)
  }

  def getIdList(filter: Option[String]) = Action {
    val json = inTransaction {
      val list = Database.getIdList()
      generate(Map("databases" -> list.iterator))
    }
    Ok(json).as(JSON)
  }

  def addDatabase() = Action { implicit request =>
    val form = getMainForm().bindFromRequest
      form.value map { database =>
      try {
        inTransaction{
          val json = generate(database.insert())
          Created(json).as(JSON)
        }
      } catch {
        case e: Exception => InternalServerError(e.toString)
      }
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

  def getDatabaseDetails(id: String) = Action {
    inTransaction(Try{
      generate(Database.getDetailed(id))
    }.toOption match {
      case Some(json) => Ok(json).as(JSON)
      case None =>
        val json = generate(Map("error"->"Database not found!"))
        BadRequest(json).as(JSON)
    })
  }

  def deleteDatabase(id: String) = Action {
    inTransaction(try{
      if (Database.deleteById(id) > 0) Ok
      else {
        val json = generate(Map("error"->"Database not found!"))
        BadRequest(json).as(JSON)
      }
    } catch {
      case e: Exception => InternalServerError(e.toString)
    })
  }

  def updateDatabase(id: String) = Action { implicit request =>
    val form = getMainForm(Some(id)).bindFromRequest
    form.value map { database =>
        inTransaction(try {
          val db = database.id match {
            case `id` => database.update()
            case _ => database.rebase(id)
          }
          val json = generate(db)
          Ok(json).as(JSON)
        } catch {
          case e:Exception => InternalServerError(e.toString)
        })
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }
}
