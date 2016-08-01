name := "user-model"

organization := "dan"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.8"

resolvers ++= Seq(
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.6",
  "codecraft" %% "codegen" % "1.0.0-SNAPSHOT",
  "com.typesafe.play" %% "play-json" % "2.5.4"
)
