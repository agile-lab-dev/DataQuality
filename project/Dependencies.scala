import sbt._

object Dependencies {

  val sparkAvro = "com.databricks" %% "spark-avro" % Version.sparkAvro
  val sparkCsv = "com.databricks" %% "spark-csv" % Version.sparkCsv

  val scopt  = "com.github.scopt" %% "scopt" % Version.scopt
  val typesafeConfig  = "com.typesafe" % "config" % Version.typesafeConfig
  val commonLang = "org.apache.commons" % "commons-lang3" % Version.commonLang
  val commonMail = "org.apache.commons" % "commons-email" % Version.commonMail
  val jodaTime = "joda-time" % "joda-time" % Version.jodaTime
  val jodaConvert = "org.joda" % "joda-convert" % Version.jodaConvert
  val log4j = "log4j" % "log4j" % Version.log4j
  val secretsManager = "com.amazonaws" % "aws-java-sdk-secretsmanager" % Version.secretsManager

  val algebirdCore = "com.twitter" %% "algebird-core" % Version.algebird
  val isarn = "org.isarnproject" %% "isarn-sketches" % Version.isarn
  val catsCore = "org.typelevel" %% "cats-core" % Version.catsCore

  val hbaseConnector = "it.nerdammer.bigdata" % "spark-hbase-connector_2.10" % Version.hbaseConnector
  val postgres = "org.postgresql" % "postgresql" % Version.postgres
  val sqlite = "org.xerial" % "sqlite-jdbc" % Version.sqlite

  val playJson = "com.typesafe.play" %% "play-json" % Version.playJson
  val squeryl = "org.squeryl" %% "squeryl" % Version.squeryl
  val jerkson = "com.gilt" % "jerkson_2.11" % Version.jerkson
  val webjars = "org.webjars" %% "webjars-play" % Version.webjars
  val swaggerUi = "org.webjars" % "swagger-ui" % Version.swaggerUi

  val scalaTest = "org.scalatest" %% "scalatest" % Version.scalaTest % Test
  val scalaTestPlay = "org.scalatestplus.play" %% "scalatestplus-play" % Version.scalaTestPlay % Test

  val dq_common = Seq(typesafeConfig, catsCore)

  val dq_core  = Seq(algebirdCore, commonLang, commonMail, hbaseConnector, isarn, jodaTime, jodaConvert, log4j,
                  postgres, scalaTest, sparkAvro, sparkCsv, scopt, sqlite, typesafeConfig, secretsManager)

  val dq_ui = Seq(scalaTest, scalaTestPlay, playJson, jodaTime, jodaConvert, squeryl, jerkson, webjars, postgres, catsCore)

  val dq_be = Seq(scalaTest, scalaTestPlay, playJson, squeryl, postgres, jerkson, swaggerUi)

  val dq_api = Seq(scalaTest, scalaTestPlay, playJson, squeryl, postgres, jerkson, swaggerUi)

  def getSparkDependencies(sparkVersion:String): Seq[ModuleID] =
    Seq(
      "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
      "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
      "org.apache.spark" %% "spark-hive" % sparkVersion % "provided"
    )

  def getJSDependencies(ngVersion: String): Seq[ModuleID] = {
    Seq(
    "org.webjars.npm" % "angular__common" % ngVersion,
    "org.webjars.npm" % "angular__compiler" % ngVersion,
    "org.webjars.npm" % "angular__core" % ngVersion,
    "org.webjars.npm" % "angular__http" % ngVersion,
    "org.webjars.npm" % "angular__forms" % ngVersion,
    "org.webjars.npm" % "angular__router" % ngVersion,
    "org.webjars.npm" % "angular__platform-browser-dynamic" % ngVersion,
    "org.webjars.npm" % "angular__platform-browser" % ngVersion,
    "org.webjars.npm" % "angular__cdk" % "2.0.0-beta.10",
    "org.webjars.npm" % "angular__material" % "2.0.0-beta.10",
    "org.webjars.npm" % "angular__animations" % ngVersion,
    "org.webjars.npm" % "systemjs" % "0.20.14",
    "org.webjars.npm" % "rxjs" % "5.4.2",
    "org.webjars.npm" % "reflect-metadata" % "0.1.8",
    "org.webjars.npm" % "zone.js" % "0.8.4",
    "org.webjars.npm" % "core-js" % "2.4.1",
    "org.webjars.npm" % "symbol-observable" % "1.0.1",

    "org.webjars.npm" % "angular__flex-layout" % "2.0.0-beta.9",

    "org.webjars.npm" % "typescript" % "2.4.1",
    "org.webjars.npm" % "codemirror" % "5.30.0",
    "org.webjars.npm" % "ng2-codemirror" % "1.1.3",

    //tslint dependency
    "org.webjars.npm" % "types__jasmine" % "2.5.53" % "test",
    //test
    "org.webjars.npm" % "jasmine-core" % "2.6.4",
    "org.webjars.npm" % "ng2-file-upload" % "1.2.0",
    "org.webjars.npm" % "file-saver" % "1.3.8",
    "org.webjars.npm" % "types__file-saver" % "1.3.0"
    )
  }
}
