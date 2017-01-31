name := "DataQuality-framework"

version := "1.0"

scalaVersion := "2.10.6"

scalacOptions ++= Seq(
  "-target:jvm-1.7",
  "-deprecation",
  "-feature",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-language:reflectiveCalls"
)

scalacOptions ++= Seq("-Xmax-classfile-name", "225")

resolvers ++= Seq(
  Resolver.mavenLocal,
  Resolver.sonatypeRepo("public"),
  "Maven Repository" at "https://repo1.maven.org/maven2",
  "Apache Repositroy" at "https://repository.apache.org/content/repositories/releases",
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/",
  "Cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/"
)

unmanagedJars in Compile += file("lib/spark-assembly-1.6.1-hadoop2.4.0.jar")

libraryDependencies ++= Seq(
  "org.apache.hadoop" % "hadoop-client" % "2.2.0",
  "org.apache.spark" % "spark-core_2.10" % "1.6.0",
  "org.apache.spark" % "spark-sql_2.10" % "1.6.0",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "com.databricks" % "spark-csv_2.10" % "1.2.0",
  "org.apache.commons" % "commons-lang3" % "3.0",
  "joda-time" % "joda-time" % "2.8.1",
  "com.github.scopt" %% "scopt" % "3.2.0",
  "log4j" % "log4j" % "1.2.17",
  "com.typesafe" % "config" % "1.2.1"

)


assemblyExcludedJars in assembly := (fullClasspath in assembly).value.filter(_.data.getName startsWith "spark-assembly")

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = true)

test in assembly := {}