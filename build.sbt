import sbt._
import com.typesafe.sbt.SbtNativePackager.autoImport.NativePackagerHelper._
import sbt.Keys.scalaVersion

ThisBuild / organization := "it.agilelab"
ThisBuild / version := "1.3.2"

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-deprecation",
  "-feature",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-language:reflectiveCalls",
  "-Xmax-classfile-name", "225"
)

resolvers ++= Seq(
  Resolver.bintrayRepo("webjars","maven"),
  Resolver.sonatypeRepo("public"),
  Resolver.jcenterRepo,
  "Maven Repository" at "https://repo1.maven.org/maven2",
  "Apache Repositroy" at "https://repository.apache.org/content/repositories/releases",
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
  "Cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/"
)

/*
  MODULE: "DQ_COMMON"
 */
lazy val common =
  (project in file("dq-common"))
    .settings(libraryDependencies ++= Dependencies.dq_common)

/*
  MODULE: "DQ_CORE"
 */

lazy val core = (project in file("dq-core"))
  .enablePlugins(UniversalPlugin, UniversalDeployPlugin)
  .settings(
    libraryDependencies ++= {
      Dependencies.dq_core ++ Dependencies.getSparkDependencies(sparkVersion.value)
    },
    Compile / unmanagedResourceDirectories += baseDirectory(_ / "src/main/resources").value,
    Compile / unmanagedJars += file("dq-core/lib/ojdbc7.jar"),

    run in Compile := Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run)).evaluated,
    runMain in Compile := Defaults.runMainTask(fullClasspath in Compile, runner in(Compile, run)).evaluated,
    parallelExecution in Test := false,
    assembly / assemblyJarName := s"dq-${name.value}_${sparkVersion.value}_${scalaVersion.value}-${version.value}.jar",
    assembly / test := {},
    assembly / assemblyMergeStrategy := {
      case PathList("org","aopalliance", xs @ _*) => MergeStrategy.last
      case PathList("javax", "inject", xs @ _*) => MergeStrategy.last
      case PathList("javax", "servlet", xs @ _*) => MergeStrategy.last
      case PathList("javax", "activation", xs @ _*) => MergeStrategy.last
      case PathList("org", "apache", xs @ _*) => MergeStrategy.last
      case PathList("com", "google", xs @ _*) => MergeStrategy.last
      case PathList("com", "esotericsoftware", xs @ _*) => MergeStrategy.last
      case PathList("com", "codahale", xs @ _*) => MergeStrategy.last
      case PathList("com", "yammer", xs @ _*) => MergeStrategy.last
      case "about.html" => MergeStrategy.rename
      case "META-INF/ECLIPSEF.RSA" => MergeStrategy.last
      case "META-INF/mailcap" => MergeStrategy.last
      case "META-INF/mimetypes.default" => MergeStrategy.last
      case "plugin.properties" => MergeStrategy.last
      case "log4j.properties" => MergeStrategy.last
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },

    Universal / mappings += {
      val confFile = buildEnv.value match {
        case BuildEnv.Stage => "conf/qa.conf"
        case BuildEnv.Test => "conf/test.conf"
        case BuildEnv.Production => "conf/prod.conf"
        case BuildEnv.Dev => "conf/dev.conf"
      }
      ((resourceDirectory in Compile).value / confFile) -> "conf/application.conf"
    },
    Universal / mappings ++= {
      val integrationFolder = integrationEnv.value match {
        case _ => "integration/dev"
      }
      directory((resourceDirectory in Compile).value / integrationFolder / "bin") ++
        directory((resourceDirectory in Compile).value / integrationFolder / "conf")
    },
    Universal / mappings += (assembly in Compile).map(jar => jar -> ("lib/" + jar.getName)).value
  )

/*
  MODULE: "DQ_UI"
 */
lazy val ui = (project in file("dq-ui"))
  .enablePlugins(PlayScala)
  .settings(
    scalaVersion := "2.11.12",
    updateOptions := updateOptions.value.withCachedResolution(cachedResoluton = true),
    //we use nodejs to make our typescript build as fast as possible
    JsEngineKeys.engineType := JsEngineKeys.EngineType.Node,
    libraryDependencies ++= {
      Seq(jdbc, cache, ws, specs2 % Test, evolutions, guice) ++ Dependencies.dq_ui ++ Dependencies.getJSDependencies("4.4.4")
    },
    dependencyOverrides += "org.webjars.npm" % "minimatch" % "3.0.0",
    // use the webjars npm directory (target/web/node_modules ) for resolution of module imports of angular2/core etc
    resolveFromWebjarsNodeModulesDir := true,
    // compile our tests as commonjs instead of systemjs modules
    typescript / projectTestFile := Some("tsconfig.test.json")
  ).dependsOn(common)

/*
  MODULE: "DQ_API"
 */
lazy val api = (project in file("dq-api"))
  .settings(
    scalaVersion := "2.11.12",
    updateOptions := updateOptions.value.withCachedResolution(cachedResoluton = true),
    libraryDependencies ++= {
      Seq(jdbc, cache, ws, specs2 % Test, evolutions, guice) ++ Dependencies.dq_api
    }
  )

/*
  MODULE: "DQ_BE"
 */
lazy val be = (project in file("dq-be"))
  .enablePlugins(PlayScala)
  .settings(
    scalaVersion := "2.11.12",
    updateOptions := updateOptions.value.withCachedResolution(cachedResoluton = true),
    libraryDependencies ++= {
      Seq(jdbc, cache, ws, specs2 % Test, evolutions, guice) ++ Dependencies.dq_be
    }
  ).dependsOn(api,common)
