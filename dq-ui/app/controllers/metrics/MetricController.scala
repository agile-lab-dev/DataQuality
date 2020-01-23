package controllers.metrics

import javax.inject.Inject
import com.codahale.jerkson.Json.generate
import controllers.utils.MyDBSession
import models.metrics.Metric.MetricType
import models.metrics.{ColumnMetric, ComposedMetric, FileMetric, Metric}
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import play.api.Configuration
import play.api.mvc.{Action, Controller}

import scala.util.Try

/**
  * Created by Egor Makhov on 28/08/2017.
  */
class MetricController @Inject()(val configuration: Configuration,session: MyDBSession) extends Controller {

  private implicit val pageLength: Option[Int] = configuration.getInt("pagination.length")

  def getAllMetrics(source: Option[String], page: Option[Int], filter: Option[String]) = Action {
    val mtType = Try(MetricType.withName(filter.get.toUpperCase)).toOption
    val json: String = inTransaction {
      val query: Query[Metric] = source match {
        case Some(src) => Metric.getMetricsBySource(src, mtType)
        case None => Metric.getAll(mtType)
      }
      val stats = Metric.getStats(query)
      val res: Query[Metric] = (pageLength,page,query) match {
        case (Some(length), Some(pg), qr: Query[Metric]) => qr.page(pg * length, length)
        case (_,_,_) => query
      }
      generate(Map("metrics" -> res.map(Metric.toShortMap)) ++ stats)
    }
    Ok(json).as(JSON)
  }

  def deleteMetricById(id: String) = Action {
    inTransaction(try{
      val delOpt: Option[Int] = Try {
        val src: String = Metric.getDetailed(id).mType
        src match {
          case "FILE" => FileMetric.deleteById(id)
          case "COLUMN" => ColumnMetric.deleteById(id)
          case "COMPOSED" => ComposedMetric.deleteById(id)
          case _ => 0
        }
      }.toOption
      delOpt match {
        case Some(x) if x > 0 => Ok
        case _ =>
          val json = generate(Map("error"->"Metric not found!"))
          BadRequest(json).as(JSON)
      }
    } catch {
      case e: Exception => InternalServerError(e.toString)
    })
  }

  def getIdList(filter: Option[String]) = Action {
    val mtType = Try(MetricType.withName(filter.get.toUpperCase)).toOption
    val json = inTransaction {
      val list: Query[String] = Metric.getIdList(mtType)
      generate(Map("metrics" -> list.iterator))
    }
    Ok(json).as(JSON)
  }

}
