package src.main.scala

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin


/** sets the build environment */
object BuildEnvPlugin extends AutoPlugin {

  // make sure it triggers automatically
  override def trigger = AllRequirements
  override def requires = JvmPlugin

  object autoImport {
    object BuildEnv extends Enumeration {
      val Production, Stage, Test, Dev = Value
    }

    val buildEnv = settingKey[BuildEnv.Value]("the current build environment")
  }
  import autoImport._

  override def projectSettings: Seq[Setting[_]] = Seq(
    buildEnv := {
      sys.props.get("env")
        .orElse(sys.env.get("BUILD_ENV"))
        .flatMap {
          case "dev" => Some(BuildEnv.Dev)
          case _ => None
        }
        .getOrElse(BuildEnv.Dev)
    },
    // give feed back
    onLoadMessage := {
      // depend on the old message as well
      val defaultMessage = onLoadMessage.value
      val env = buildEnv.value
      s"""|$defaultMessage
          |Running in build environment: $env""".stripMargin
    }
  )

}
