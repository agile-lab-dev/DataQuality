import sbt.addSbtPlugin

// General plugins
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin"    % "5.2.4") // Eclipse compatibility
addSbtPlugin("org.scalameta"           % "sbt-scalafmt"         % "2.2.1")
addSbtPlugin("net.virtual-void"        % "sbt-dependency-graph" % "0.10.0-RC1")
addSbtPlugin("com.timushev.sbt"        % "sbt-updates"          % "0.5.0")

addSbtPlugin("com.eed3si9n"     % "sbt-assembly"        % "0.14.10") // Creates fat Jars
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.6.1") // Creates universal packages
addSbtPlugin("org.scoverage"    % "sbt-scoverage"       % "1.6.1")

// Web application plugins
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.7.3")
addSbtPlugin("com.typesafe.sbt"  % "sbt-digest" % "1.1.4")

addSbtPlugin("name.de-vries" % "sbt-typescript" % "2.6.2")
addSbtPlugin("name.de-vries" % "sbt-tslint"     % "5.7.0")
addSbtPlugin("name.de-vries" % "sbt-jasmine"    % "0.0.4")
