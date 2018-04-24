import sbt.addSbtPlugin

logLevel := Level.Warn

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "4.0.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.0")

//// Cats support for Scala 2.10
addSbtPlugin("org.lyranthe.sbt" % "partial-unification" % "1.1.0")

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.14")

// provides server side compilation of typescript to ecmascript 5 or 3
addSbtPlugin("name.de-vries" % "sbt-typescript" % "2.5.2")
// checks your typescript code for error prone constructions
addSbtPlugin("name.de-vries" % "sbt-tslint" % "5.1.0")
// runs jasmine tests
addSbtPlugin("name.de-vries" % "sbt-jasmine" % "0.0.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.0")
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.10")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")