package controllers

import javax.inject.Inject
import org.webjars.play.{RequireJS, WebJarAssets}
import play.api.mvc._

class Application @Inject() (webjars: WebJarAssets, requirejs: RequireJS) extends Controller {

  def index = Action {

    Ok(views.html.index())
  }

}