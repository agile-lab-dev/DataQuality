package controllers.search

import javax.inject.Inject
import com.codahale.jerkson.Json.generate
import controllers.utils.MyDBSession
import models.{BasicEntityType, EntityParentEnumeration}
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import play.api.Configuration
import play.api.mvc.{Action, AnyContent, Controller}

import scala.util.Try


class SearchController @Inject()(val configuration: Configuration,session: MyDBSession) extends Controller {

  private implicit val pageLength: Option[Int] = configuration.getInt("pagination.length")

  def searchById(tipo: String, query: String, filter: Option[String], parent: Option[String], parentId:Option[String], page: Option[Int]): Action[AnyContent] = Action {
    inTransaction { try {
      val eType: Option[BasicEntityType.Value] = Try(BasicEntityType.withName(tipo.toLowerCase)).toOption
      val result: Map[_ <: String, Any] = eType match {
        case Some(t) =>
          val tipo: Option[Enumeration#Value] = Try(t.service.typeEnum.get.withName(filter.get.toUpperCase)).toOption
          val parentType: Option[EntityParentEnumeration#EntityParentVal] = Try(t.service.parentEnum.get.withName(parent.get.toLowerCase).asInstanceOf[EntityParentEnumeration#EntityParentVal]).toOption
          val q: Query[String] = t.service.searchIdList('%'+query+'%', tipo, parentType, parentId)
          val res: Query[String] = (pageLength,page) match {
            case (Some(length), Some(pg)) => q.page(pg * length, length)
            case (_,_) => q
          }
          Map("results" -> res.toList, "size" -> q.iterator.size, "last_page" -> Try((q.iterator.size - 1)/pageLength.get).toOption)
        case None => Map.empty
      }
      val json = generate(result)
      Ok(json).as(JSON)
    } catch {
      case e:Exception => InternalServerError(e.toString)
    }}
  }

}
