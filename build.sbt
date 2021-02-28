name := """home"""
organization := "home"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.12"

/*libraryDependencies += guice*/
/*libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.20"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.4.20"*/

libraryDependencies ++= Seq(
  ws,
  "com.h2database" % "h2" % "1.4.200",
  "org.postgresql" % "postgresql" % "42.2.9",
  "org.joda" % "joda-money" % "0.12" withSources(),

  "org.scalatest" % "scalatest_2.11" % "3.0.0" % "test" withSources(),
  "com.google.inject" % "guice" % "4.2.0",
  "com.typesafe.akka" %% "akka-actor" % "2.4.8" withSources() withJavadoc(), 
  "com.typesafe.play" % "play_2.11" % "2.5.14" withSources(), 
  "joda-time" % "joda-time" % "2.9.3" withSources()
  
)
// Adds additional packages into Twirl
//TwirlKeys.templateImports += "home.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "home.binders._"
