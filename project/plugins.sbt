// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.19")

//Migration manager: The latest version of MiMa's sbt plugin supports sbt 1.3.0+. Use v0.6.0 for sbt 1.0.x - 1.2.x, and v0.3.0 for sbt 0.13.x.
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.3.0")