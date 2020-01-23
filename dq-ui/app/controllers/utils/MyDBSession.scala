package controllers.utils

import javax.inject.{Inject, Singleton}
import org.squeryl.adapters.{H2Adapter, PostgreSqlAdapter}
import org.squeryl.internals.DatabaseAdapter
import org.squeryl.{Session, SessionFactory}
import play.api.Configuration
import play.api.db.Database

@Singleton
class MyDBSession @Inject()(configuration: Configuration, db:Database) {


  def getSession(adapter:DatabaseAdapter) = Session.create(db.getConnection() , adapter)

        SessionFactory.concreteFactory = configuration.getString("db.default.driver") match {
          case Some("org.h2.Driver") => Some(() => getSession(new H2Adapter))
          case Some("org.postgresql.Driver") => Some(() => getSession(new PostgreSqlAdapter))
          case _ => sys.error("Database driver must be either org.h2.Driver or org.postgresql.Driver")
        }
}

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
