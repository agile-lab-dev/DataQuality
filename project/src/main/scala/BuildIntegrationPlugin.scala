package src.main.scala

import sbt.Keys.onLoadMessage
import sbt.plugins.JvmPlugin
import sbt.{AllRequirements, AutoPlugin, Setting, settingKey}

/** sets the build environment */
object BuildIntegrationPlugin extends AutoPlugin {

  // make sure it triggers automatically
  override def trigger = AllRequirements
  override def requires = JvmPlugin

  object autoImport {
    object IntegrationEnv extends Enumeration {
      val dev = Value
    }

    val integrationEnv = settingKey[IntegrationEnv.Value]("the current build integration environment")
  }
  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    integrationEnv := {
      sys.props.get("integration")
        .orElse(sys.env.get("BUILD_ENV"))
        .flatMap {
          case "dev" => Some(IntegrationEnv.dev)
          case _ => None
        }
        .getOrElse(IntegrationEnv.dev)
    },
    // give feed back
    onLoadMessage := {
      // depend on the old message as well
      val defaultMessage = onLoadMessage.value
      val env = integrationEnv.value
      s"""|$defaultMessage
          |Running in integration environment: $env""".stripMargin
    }
  )

}
