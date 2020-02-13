import sbt._

object Dependencies {

  val algebirdCore = "com.twitter" %% "algebird-core" % Version.algebird

  val catsCore = "org.typelevel" %% "cats-core" % Version.catsCore

  val commonLang = "org.apache.commons" % "commons-lang3" % Version.commonLang
  val commonMail = "org.apache.commons" % "commons-email" % Version.commonMail

  val hbaseConnector = "it.nerdammer.bigdata" % "spark-hbase-connector_2.10" % Version.hbaseConnector

  val isanr = "org.isarnproject" %% "isarn-sketches" % Version.isarn

  val jodaTime = "joda-time" % "joda-time" % Version.jodaTime
  val jodaConvert = "org.joda" % "joda-convert" % Version.jodaConvert

  val log4j = "log4j" % "log4j" % Version.log4j

  val postgres = "org.postgresql" % "postgresql" % Version.postgres

  val scalaTest = "org.scalatest" %% "scalatest" % Version.scalaTest % "test"

  val sparkAvro = "com.databricks" %% "spark-avro" % Version.sparkAvro
  val sparkCsv = "com.databricks" %% "spark-csv" % Version.sparkCsv

  val scopt  = "com.github.scopt" %% "scopt" % Version.scopt

  val sqlite = "org.xerial" % "sqlite-jdbc" % Version.sqlite

  val typesafeConfig  = "com.typesafe" % "config" % Version.typesafeConfig


  val dq_core  = Seq(algebirdCore, commonLang, commonMail, hbaseConnector, isanr, jodaTime, jodaConvert, log4j,
                  postgres, scalaTest, sparkAvro, sparkCsv, scopt, sqlite, typesafeConfig)

  val dq_common = Seq(typesafeConfig, catsCore)


  def sparkDependenciesCalculation(sparkVersion:String): Seq[ModuleID] =
    Seq(
      "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
      "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
      "org.apache.spark" %% "spark-hive" % sparkVersion % "provided"
    )

}
