
name := "hub"

version := "0.1"
scalaVersion := "2.13.2"
mainClass in assembly := Some("eu.jrie.put.wti.bigstore.hub.HubService")

libraryDependencies ++= Seq(
  // akka
  "com.typesafe.akka" %% "akka-actor-typed" % "2.6.4",
  "com.typesafe.akka" %% "akka-stream" % "2.6.4",
  "com.typesafe.akka" %% "akka-http"   % "10.1.11",

  // util
  "ch.qos.logback" % "logback-classic" % "1.2.3",

  // test
  "org.scalatest" %% "scalatest" % "3.0.8" % "test"
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case "reference.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}

