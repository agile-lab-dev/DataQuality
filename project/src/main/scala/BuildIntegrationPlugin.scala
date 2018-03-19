package src.main.scala

import sbt.Keys.onLoadMessage
import sbt.plugins.JvmPlugin
import sbt.{AllRequirements, AutoPlugin, Setting, settingKey}

/** Simple plugin to control project integration configurations */
object BuildIntegrationPlugin extends AutoPlugin {

  // make sure it triggers automatically
  override def trigger = AllRequirements
  override def requires = JvmPlugin

  object autoImport {
    object IntegrationEnv extends Enumeration {
      val local = Value
    }

    val integrationEnv = settingKey[IntegrationEnv.Value](
      "the current build integration environment")
  }
  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    integrationEnv := {
      sys.props
        .get("integration")
        .orElse(sys.env.get("INTEGRATION"))
        .flatMap {
          case "local" => Some(IntegrationEnv.local)
          //todo: Add more if needed
          case _ => None
        }
        .getOrElse(IntegrationEnv.local)
    },
    onLoadMessage := {
      val defaultMessage = onLoadMessage.value
      val env = integrationEnv.value
      s"""|$defaultMessage
          |Running in integration environment: $env""".stripMargin
    }
  )

}
