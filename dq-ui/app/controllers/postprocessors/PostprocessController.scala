package controllers.postprocessors

import com.codahale.jerkson.Json.generate
import javax.inject.Inject
import models.sources.Source
import models.sources.Source.SourceType
import org.squeryl.PrimitiveTypeMode.inTransaction
import org.squeryl.Query
import play.api.Configuration
import play.api.mvc.{Action, Controller}

import scala.util.Try

/**
  * Created by Egor Makhov on 07/08/2017.
  */
class PostprocessController @Inject()(val configuration: Configuration) extends Controller {

  private implicit val pageLength: Option[Int] = configuration.getInt("pagination.length")

  def getAllPPs() = Action {
    val testJSON = """test : {}"""
    Ok(testJSON).as(JSON)
  }

}

