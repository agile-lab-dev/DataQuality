package controllers.sources

import javax.inject.Inject
import com.codahale.jerkson.Json.generate
import controllers.utils.MyDBSession
import models.sources.Source.SourceType
import models.sources._
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import play.api.Configuration
import play.api.mvc.{Action, Controller}

import scala.util.Try

/**
  * Created by Egor Makhov on 24/08/2017.
  */
class SourceController @Inject()(val configuration: Configuration,session: MyDBSession) extends Controller {

  private implicit val pageLength: Option[Int] = configuration.getInt("pagination.length")

  def getAllSources(database: Option[String], page: Option[Int], filter: Option[String]) = Action {
    val scType = Try(SourceType.withName(filter.get.toUpperCase)).toOption
    val json: String = inTransaction {
      val query: Query[Source] = database match {
        case Some(db) => Source.getSourcesByDatabase(db)
        case None => Source.getAll(scType)
      }
      val stats = Source.getStats(query)
      val res = (pageLength,page) match {
        case (Some(length), Some(pg)) => query.page(pg * length, length)
        case (_,_) => query
      }
      generate(Map("sources" -> res.map(Source.toShortMap)) ++ stats)
    }
    Ok(json).as(JSON)
  }

  def getIdList(filter: Option[String]) = Action {
    val scType = Try(SourceType.withName(filter.get.toUpperCase)).toOption
    val json = inTransaction {
      val list = Source.getIdList(scType)
      generate(Map("sources" -> list.iterator))
    }
    Ok(json).as(JSON)
  }

  def deleteSourceById(id: String) = Action {
    inTransaction(try{
      val delOpt: Option[Int] = Try {
        val src: String = Source.getDetailed(id).scType
        src match {
          case "HDFS" => HdfsFile.deleteById(id)
          case "TABLE" => DBTable.deleteById(id)
          case "HIVE" => HiveTable.deleteById(id)
          case "VIRTUAL" => VirtualSource.deleteById(id)
          case _ => 0
        }
      }.toOption
      delOpt match {
        case Some(x) if x > 0 => Ok
        case _ =>
          val json = generate(Map("error"->"Source not found!"))
          BadRequest(json).as(JSON)
      }
    } catch {
      case e: Exception => InternalServerError(e.toString)
    })
  }
}
