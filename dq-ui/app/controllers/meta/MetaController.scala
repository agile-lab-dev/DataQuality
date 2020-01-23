package controllers.meta

import javax.inject.Inject
import com.codahale.jerkson.Json.generate
import controllers.utils.MyDBSession
import controllers.utils.ResultWrappers._
import models.checks.Check
import models.checks.Check.CheckType
import models.meta.{CheckMeta, MetricMeta}
import models.metrics.Metric
import models.metrics.Metric.MetricType
import org.squeryl.PrimitiveTypeMode._
import play.api.mvc.{Action, Controller}

import scala.util.Try

/**
  * Created by Egor Makhov on 19/10/2017.
  */
class MetaController @Inject()(session: MyDBSession) extends Controller {

  private object MetaType extends Enumeration {
    type MetaType = Value
    val metric = Value("METRIC")
    val check = Value("CHECK")
  }

  def getMetaById(id: String, tipo: String) = Action {
    safeResultInTransaction {
      val metaType = Try(MetaType.withName(tipo.toUpperCase)).toOption
      metaType match {
        case Some(MetaType.metric) =>
          val json = generate(MetricMeta.getById(id).iterator)
          Ok(json).as(JSON)
        case Some(MetaType.check) =>
          val json = generate(CheckMeta.getById(id).iterator)
          Ok(json).as(JSON)
        case None =>
          val json = generate(Map("error"->"Meta information not found!"))
          BadRequest(json).as(JSON)
      }
    }
  }

  def getMetaByType(tipo: String, filter: Option[String] = None) = Action {
    safeResultInTransaction {
      val metaType = Try(MetaType.withName(tipo.toUpperCase)).toOption
      metaType match {
        case Some(MetaType.metric) =>
          val mtType = Try(MetricType.withName(filter.get.toUpperCase)).toOption
          val json = generate(MetricMeta.all(mtType).iterator)
          Ok(json).as(JSON)
        case Some(MetaType.check) =>
          val chkType = Try(CheckType.withName(filter.get.toUpperCase)).toOption
          val json = generate(CheckMeta.all(chkType).iterator)
          Ok(json).as(JSON)
        case None =>
          val json = generate(Map("error" -> "Meta information not found!"))
          BadRequest(json).as(JSON)
      }
    }
  }

  def getShortList(tipo: String, filter:Option[String] = None) = Action {
    safeResultInTransaction {
      val metaType = Try(MetaType.withName(tipo.toUpperCase)).toOption
      metaType match {
        case Some(MetaType.metric) =>
          val mtType = Try(MetricType.withName(filter.get.toUpperCase)).toOption
          val json = generate(MetricMeta.getShortList(mtType).iterator)
          Ok(json).as(JSON)
        case Some(MetaType.check) =>
          val chkType = Try(CheckType.withName(filter.get.toUpperCase)).toOption
          val json = generate(CheckMeta.getShortList(chkType).iterator)
          Ok(json).as(JSON)
        case None =>
          val json = generate(Map("error"->"Meta information not found!"))
          BadRequest(json).as(JSON)
      }
    }
  }

  def getTrendCheckRules() = Action {
    safeResultInTransaction {
      val json = generate(CheckMeta.getTrendCheckRules.iterator)
      Ok(json).as(JSON)
    }
  }

  // TODO: Drop this workaround
  def getMetaForInstance(entity: String, id: String, filter: Option[String]) = Action {

    val topNChecks = Seq("TOP_N_RANK_CHECK")

    safeResultInTransaction {
      val metaType = Try(MetaType.withName(entity.toUpperCase)).toOption
      metaType match {
        case Some(MetaType.metric) =>
          val chkType: Option[Check.CheckType.Value] = Try(CheckType.withName(filter.get.toUpperCase)).toOption
          val metas: Seq[String] = CheckMeta.getShortList(chkType).toList
          val json = generate( if (Metric.getTopNList.contains(id)) topNChecks else metas.filter(!topNChecks.contains(_)) )
          Ok(json).as(JSON)
        case _ =>
          val json = generate(Map("error"->"Meta information not found!"))
          BadRequest(json).as(JSON)
      }
    }
  }
}
