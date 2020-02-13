import sbt._
import Multiversion.sparkVersion
import com.typesafe.sbt.SbtNativePackager.autoImport.NativePackagerHelper._
import sbt.Keys.{scalaVersion, test}
import sbtassembly.AssemblyPlugin.autoImport.{assemblyExcludedJars, assemblyOption}

lazy val commonSettings = Seq(
  version := "1.2.1"
)

sparkVersion := "2.4.0" // default spark version

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

def calcVersionScala(sparkVersion: String): String = {
  sparkVersion.head match {
    case '1' => "2.10.6"
    case '2' => "2.11.11"
    case _ => throw new Exception("This Spark version is not supported")
  }
}

/*
  MODULE: "DQ_ROOT"
 */
lazy val root = (project in file(".")).settings(
  name := "DataQuality-framework"
).aggregate(core, common)

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
    sparkVersion := sparkVersion.all(ScopeFilter(inProjects(ProjectRef(file("."), "root")))).value.head,
    scalaVersion := calcVersionScala(sparkVersion.value),
    commonSettings,
    libraryDependencies ++=  {
      //val sv = sparkVersion.all(ScopeFilter(inProjects(ProjectRef(file("."), "root")))).value.head
      Dependencies.dq_core ++ Dependencies.sparkDependenciesCalculation(sparkVersion.value)
    },
    unmanagedResourceDirectories in Compile += baseDirectory(_ / "src/main/resources").value,
    excludeFilter in Compile in unmanagedResources := "*",
    unmanagedJars in Compile += file("dq-core/lib/ojdbc7.jar"),
    assemblyExcludedJars in assembly := (fullClasspath in assembly).value.filter(_.data.getName startsWith "spark-assembly"),
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = true),
    test in assembly := {},
    assemblyMergeStrategy in assembly := {
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
    mappings in Universal += {
      // TODO: Add paths application configuration files
      val confFile = buildEnv.value match {
        case BuildEnv.Stage => "conf/qa.conf"
        case BuildEnv.Test => "conf/test.conf"
        case BuildEnv.Production => "conf/prod.conf"
        case BuildEnv.Dev => "conf/dev.conf"
      }
      ((resourceDirectory in Compile).value / confFile) -> "conf/application.conf"
    },
    mappings in Universal ++= {
      // TODO: Add paths application integration files
      val integrationFolder = integrationEnv.value match {
        case _ => "integration/dev"
      }
      directory((resourceDirectory in Compile).value / integrationFolder / "bin") ++
        directory((resourceDirectory in Compile).value / integrationFolder / "conf")
    },
    mappings in Universal <+= (assembly in Compile) map { jar =>
      jar -> ("lib/" + jar.getName)
    },
    assemblyJarName in assembly := s"dq-core_${sparkVersion.value}_${scalaVersion.value}.jar"
  )

/*
  MODULE: "DQ_UI"
 */
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
        jdbc, cache, ws, specs2%Test, evolutions, guice,
        "com.typesafe.play" %% "play-json" % "2.5.14",
        "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % "test",
        "joda-time" % "joda-time" % "2.9.9",
        "org.joda" % "joda-convert" % "1.9.2",
        "org.squeryl" %% "squeryl" % "0.9.9",
        "com.gilt" % "jerkson_2.11" % "0.6.9",
        "org.webjars" %% "webjars-play" % "2.7.3",
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
    (projectTestFile in typescript) := Some("tsconfig.test.json")
  ).dependsOn(common)

/*
  MODULE: "DQ_API"
 */
lazy val api = (project in file("dq-api"))
  .settings(
    inThisBuild(
      commonSettings ++ List(scalaVersion := "2.11.12")
    ),
    incOptions := incOptions.value.withNameHashing(true),
    updateOptions := updateOptions.value.withCachedResolution(cachedResoluton = true),
    libraryDependencies ++= {
      val ngVersion="4.4.4"
      Seq(
        jdbc, cache, ws, specs2%Test, evolutions, guice,
        "com.typesafe.play" %% "play-json" % "2.5.14",
        "org.squeryl" %% "squeryl" % "0.9.9",
        "org.postgresql" % "postgresql" % "42.1.1",
        "com.gilt" % "jerkson_2.11" % "0.6.9",
        "org.webjars" % "swagger-ui" % "3.1.5",
        "org.scalatest"          %% "scalatest"          % "3.0.4" % Test,
        "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
      )
    }
  )

/*
  MODULE: "DQ_BE"
 */
lazy val be = (project in file("dq-be"))
  .enablePlugins(PlayScala)
  .settings(
    inThisBuild(
      commonSettings ++ List(scalaVersion := "2.11.12")
    ),
    incOptions := incOptions.value.withNameHashing(true),
    updateOptions := updateOptions.value.withCachedResolution(cachedResoluton = true),
    libraryDependencies ++= {
      val ngVersion="4.4.4"
      Seq(
        jdbc, cache, ws, specs2%Test, evolutions, guice,
        "com.typesafe.play" %% "play-json" % "2.5.14",
        "org.squeryl" %% "squeryl" % "0.9.9",
        "org.postgresql" % "postgresql" % "42.1.1",
        "com.gilt" % "jerkson_2.11" % "0.6.9",
        "org.webjars" % "swagger-ui" % "3.1.5",
        "org.scalatest"          %% "scalatest"          % "3.0.4" % Test,
        "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
      )
    }
  ).dependsOn(api,common)