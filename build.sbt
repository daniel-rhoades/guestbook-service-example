enablePlugins(JavaAppPackaging)

name := "guestbook-service-example"
organization := "com.danielrhoades"
version := "0.1"
scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV       = "2.4.5"
  val scalaTestV  = "2.2.6"
  Seq(
    "com.typesafe.akka"   %% "akka-actor" % akkaV,
    "com.typesafe.akka"   %% "akka-stream" % akkaV,
    "com.typesafe.akka"   %% "akka-http-experimental" % akkaV,
    "com.typesafe.akka"   %% "akka-http-spray-json-experimental" % akkaV,
    "com.typesafe.akka"   %% "akka-http-testkit" % akkaV,
    "org.scalatest"       %% "scalatest" % scalaTestV % "test",
    "org.mongodb.scala"   %% "mongo-scala-driver" % "1.1.1",
    "io.netty"            %   "netty-all" % "4.1.6.Final"
  )
}

Revolver.settings