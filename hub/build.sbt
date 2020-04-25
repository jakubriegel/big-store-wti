name := "hub"

version := "0.1"

scalaVersion := "2.13.2"

libraryDependencies ++= Seq(
  // akka
  "com.typesafe.akka" %% "akka-actor-typed" % "2.6.4",
  "com.typesafe.akka" %% "akka-stream" % "2.6.4",
  "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "2.0.0-RC1",
  "com.typesafe.akka" %% "akka-http"   % "10.1.11",

  // util
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)
