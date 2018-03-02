package controllers.targets

import javax.inject.Inject

import com.codahale.jerkson.Json.generate
import controllers.ControllerUtils._
import controllers.utils.ValidationConstraints._
import models.targets.Target.TargetType
import models.targets.{Mail, Target, TargetToChecks}
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
  * Created by Egor Makhov on 08/08/2017.
  */
class TargetController @Inject()(val configuration: Configuration) extends Controller {

  private implicit val pageLength: Option[Int] = configuration.getInt("pagination.length")

  private def getMainForm(currId: Option[String] = None): Form[(Target, List[Mail], List[TargetToChecks])] = {
    def valFunc: (String) => Boolean = (t:String) => inTransaction(validateId(t, Target.getIdList(), currId))

    Form(
      mapping(
        "id" -> optional(text verifying(errorUsed, valFunc)),
        "targetType" -> text,
        "fileFormat" -> text,
        "path" -> text,
        "delimiter" -> optional(text),
        "savemode" -> optional(text),
        "partitions" -> optional(number),
        "mails" -> list(
          single(
            "address" -> email
          )
        ),
        "checks" -> list(
          single(
            "checkId" -> text
          )
        )
      )(Target.applyOpt)(Target.unapplyOpt)
    )
  }

  def getAllTargets(check: Option[String], page: Option[Int], filter: Option[String]) = Action {
    val tarType = Try(TargetType.withName(filter.get.toUpperCase)).toOption
    val json: String = inTransaction {
      val query: Query[Target] = check match {
        case Some(chk) => Target.getTargetListByCheck(chk)
        case None => Target.getAll(tarType)
      }
      val stats = Target.getStats(query)
      val res = (pageLength,page) match {
        case (Some(length), Some(pg)) => query.page(pg * length, length)
        case (_,_) => query
      }
      generate(Map("targets" -> res.map(Target.toShortMap)) ++ stats)
    }

    Ok(json).as(JSON)
  }

  def getIdList(filter: Option[String]) = Action {
    val tarType = Try(TargetType.withName(filter.get.toUpperCase)).toOption
    val json = inTransaction {
      val list = Target.getIdList(tarType)
      generate(Map("targets" -> list.iterator))
    }
    Ok(json).as(JSON)
  }

  def addTarget() = Action { implicit request =>
    val form = getMainForm().bindFromRequest
    form.value map { target =>
      try {
        val json = inTransaction({
          target._1.insert()
          target._2.foreach(_.insert)
          target._3.foreach(_.insert)
          generate(target._1)
        })
        Created(json).as(JSON)
      } catch {
        case e: Exception =>
          //todo: Add checks and id validation
          println(e)
          InternalServerError(e.toString)
      }
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

  def getTargetDetails(id: String) = Action {
    Try(inTransaction {
      val tar: Target = Target.getDetailed(id)
      generate(tar)
    }).toOption match {
      case Some(json) => Ok(json).as(JSON)
      case None => BadRequest("Target not found!")
    }
  }

  def deleteTarget(id: String) = Action {
    Try(inTransaction {
      Target.deleteById(id)
    }).toOption match {
      case Some(_) => Ok
      case None => BadRequest("Some errors occur!")
    }
  }

  def updateTarget(id: String) = Action { implicit request =>
    val form = getMainForm(Some(id)).bindFromRequest
    form.value map { target =>
      try {
        val json = inTransaction({
          if (target._1.id == id) {
            Target.deleteAdditionsById(id)
            target._1.update()
          } else {
            Target.deleteById(id)
            target._1.insert()
          }
          target._2.foreach(_.insert)
          target._3.foreach(_.insert)
          generate(target._1)
        })
        Ok(json).as(JSON)
      } catch {
        case e: Exception =>
          //todo: Add checks and id validation
          println(e)
          InternalServerError(e.toString)
      }
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }
}
