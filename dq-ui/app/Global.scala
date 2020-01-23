import org.squeryl.adapters.{H2Adapter, PostgreSqlAdapter}
import org.squeryl.internals.DatabaseAdapter
import org.squeryl.{Query, Session, SessionFactory}
import play.api.db.NamedDatabase

import org.squeryl.PrimitiveTypeMode.inTransaction
import play.api.Application
import play.api.mvc._

import scala.concurrent.Future

/**
  * Created by Egor Makhov on 25/07/2017.
  */

//object Global extends GlobalSettings {
//
//  override def onStart(app: Application) {
//      SessionFactory.concreteFactory = app.configuration.getString("db.default.driver") match {
//      case Some("org.h2.Driver") => Some(() => getSession(new H2Adapter, app))
//      case Some("org.postgresql.Driver") => Some(() => getSession(new PostgreSqlAdapter, app))
//      case _ => sys.error("Database driver must be either org.h2.Driver or org.postgresql.Driver")
//    }
//  }
//
//  override def onHandlerNotFound(request: RequestHeader): Future[Result] = {
//    Future.successful{
//      Results.Redirect("/dataquality")
//    }
//  }
//
//  def getSession(adapter:DatabaseAdapter, app: Application) = Session.create(DB.getConnection()(app), adapter)
//}