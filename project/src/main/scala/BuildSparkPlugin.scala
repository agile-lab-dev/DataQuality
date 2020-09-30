package src.main.scala

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

/** sets the spark and scala versions */
object BuildSparkPlugin extends AutoPlugin {
  object autoImport {
    val sparkVersion = settingKey[String]("The version of Apache Spark used for building")
  }

  import autoImport._

  // make sure it triggers automatically
  override def trigger = AllRequirements
  override def requires = JvmPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    sparkVersion := "2.4.0", // warning
    scalaVersion := {
      sparkVersion.value match {
        case v if v >= "2.4.5" => "2.12.10"
        case v if v >= "2.0.0" => "2.11.12"
        case v if v >= "1.6.0" => "2.10.7"
        case _ => "2.11.12" // default
      }
    },
    onLoadMessage := {
      s"""|${onLoadMessage.value}
          |Current Spark version: ${sparkVersion.value}
          |Current Scala version: ${scalaVersion.value}""".stripMargin
    }
  )

}
