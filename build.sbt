import com.typesafe.sbt.packager.MappingsHelper.directory
import sbt.GlobFilter
import sbt.Keys.{logLevel, scalaVersion, test, updateOptions}
import sbtassembly.AssemblyPlugin.autoImport.assemblyOption
import src.main.scala.BuildEnvPlugin.autoImport.{BuildEnv, buildEnv}
import src.main.scala.BuildIntegrationPlugin.autoImport.{IntegrationEnv, integrationEnv}

name := "DataQuality-framework"

lazy val commonSettings = Seq(version := "0.2.1")

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-deprecation",
  "-feature",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-language:reflectiveCalls",
  "-Xmax-classfile-name", "225"
//  "-Ypartial-unification"
)

resolvers ++= Seq(
  Resolver.bintrayRepo("webjars","maven"),
  Resolver.sonatypeRepo("public"),
  Resolver.jcenterRepo,
  "Maven Repository" at "https://repo1.maven.org/maven2",
  "Apache Repositroy" at "https://repository.apache.org/content/repositories/releases",
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
)

//makeDeploymentSettings(Universal, packageBin in Universal, "zip")

lazy val common = (project in file("dq-common"))
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.3.1",
      "org.typelevel" %% "cats-core" % "1.1.0"
    )
  )

lazy val core = (project in file("dq-core"))
  .enablePlugins(UniversalPlugin, UniversalDeployPlugin)
  .settings(
//    inThisBuild(
//      commonSettings ++ List(scalaVersion := "2.10.6")
//    ),
    scalaVersion := "2.10.6",
    commonSettings,
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "1.6.0",
      "org.apache.spark" %% "spark-sql" % "1.6.0",
      "org.apache.spark" %% "spark-hive" % "1.6.0",
      "com.databricks" %% "spark-avro" % "2.0.1",
      "com.databricks" %% "spark-csv" % "1.5.0",
      "org.apache.commons" % "commons-lang3" % "3.0",
      "joda-time" % "joda-time" % "2.9.9",
      "org.joda" % "joda-convert" % "1.9.2",
      "com.github.scopt" %% "scopt" % "3.2.0",
      "log4j" % "log4j" % "1.2.17",
      "com.typesafe" % "config" % "1.3.1",
      "org.isarnproject" %% "isarn-sketches" % "0.0.2",
      "org.xerial" % "sqlite-jdbc" % "3.8.11.2",
      "org.postgresql" % "postgresql" % "42.1.1",
      "com.twitter" %% "algebird-core" % "0.13.0",
      "org.apache.commons" % "commons-email" % "1.4",
      "it.nerdammer.bigdata" % "spark-hbase-connector_2.10" % "1.0.3",
      "org.scalatest" %% "scalatest" % "2.2.1" % "test"
    ),
    unmanagedResourceDirectories in Compile <+= baseDirectory(_ / "src/main/resources"),
    excludeFilter in Compile in unmanagedResources := "*",
    unmanagedJars in Compile += file("dq-core/lib/ojdbc7.jar"),
    resolvers ++= Seq(
      "Cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/",
      "isarn project" at "https://dl.bintray.com/isarn/maven/"
    ),
    assemblyExcludedJars in assembly := (fullClasspath in assembly).value.filter(_.data.getName startsWith "spark-assembly"),
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = true),
    test in assembly := {},
    mappings in Universal += {
      val confFile = buildEnv.value match {
        case BuildEnv.Dev => "path to application.conf"
        case BuildEnv.Test => "path to application.conf"
        case BuildEnv.Production => "path to application.conf"
      }
      ((resourceDirectory in Compile).value / confFile) -> "conf/application.conf"
    },
    mappings in Universal ++= {
      val integrationFolder = integrationEnv.value match {
        case IntegrationEnv.local => "path to integration directory"
      }
      directory((resourceDirectory in Compile).value / integrationFolder / "bin") ++
        directory((resourceDirectory in Compile).value / integrationFolder / "conf")
    },
    mappings in Universal <+= (assembly in Compile) map { jar =>
      jar -> ("lib/" + jar.getName)
    }
  )

lazy val ui = (project in file("dq-ui"))
  .enablePlugins(PlayScala)
  .settings(
    inThisBuild(
      commonSettings ++ List(scalaVersion := "2.11.12")
    ),
    incOptions := incOptions.value.withNameHashing(true),
    updateOptions := updateOptions.value.withCachedResolution(cachedResoluton = true),
    //we use nodejs to make our typescript build as fast as possible
    JsEngineKeys.engineType := JsEngineKeys.EngineType.Node,
    libraryDependencies ++= {
      val ngVersion="4.4.4"
      Seq(
        jdbc, cache, ws, specs2%Test, evolutions,
        "com.typesafe.play" %% "play-json" % "2.5.14",
        "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % "test",
        "joda-time" % "joda-time" % "2.9.9",
        "org.joda" % "joda-convert" % "1.9.2",
        "org.squeryl" %% "squeryl" % "0.9.9",
        "com.gilt" % "jerkson_2.11" % "0.6.9",
        "org.webjars" %% "webjars-play" % "2.5.0",
        "org.postgresql" % "postgresql" % "42.1.1",
        "org.typelevel" %% "cats-core" % "1.1.0",

        //angular2 dependencies
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
        "org.webjars.npm" % "tslint-eslint-rules" % "3.4.0",
        "org.webjars.npm" % "tslint-microsoft-contrib" % "4.0.0",
        "org.webjars.npm" % "types__jasmine" % "2.5.53" % "test",
        //test
        "org.webjars.npm" % "jasmine-core" % "2.6.4",
        "org.webjars.npm" % "ng2-file-upload" % "1.2.0",
        "org.webjars.npm" % "file-saver" % "1.3.8",
        "org.webjars.npm" % "types__file-saver" % "1.3.0"
      )
    },
    dependencyOverrides += "org.webjars.npm" % "minimatch" % "3.0.0",

    // use the webjars npm directory (target/web/node_modules ) for resolution of module imports of angular2/core etc
    resolveFromWebjarsNodeModulesDir := true,

    // compile our tests as commonjs instead of systemjs modules
    (projectTestFile in typescript) := Some("tsconfig.test.json"),

    // use the combined tslint and eslint rules plus ng2 lint rules
    (rulesDirectories in tslint) := Some(List(
    tslintEslintRulesDir.value,
    // codelyzer uses 'cssauron' which can't resolve 'through' see https://github.com/chrisdickinson/cssauron/pull/10
    ng2LintRulesDir.value
    )),

    // the naming conventions of our test files
    jasmineFilter in jasmine := GlobFilter("*Test.js") | GlobFilter("*Spec.js") | GlobFilter("*.spec.js"),
    logLevel in jasmine := Level.Info,
    logLevel in tslint := Level.Info,
    logLevel in typescript := Level.Info
  ).dependsOn(common)
