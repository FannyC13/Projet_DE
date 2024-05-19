val scala3Version = "3.3.3"

lazy val root = project
  .in(file("."))
  .settings(
    name := "scala3",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.10" % Test,
    libraryDependencies += "org.json4s" %% "json4s-native" % "4.0.3",
    libraryDependencies += "com.lihaoyi" %% "upickle" % "3.3.0",
    libraryDependencies += "org.apache.kafka" % "kafka-clients" % "3.2.0",
    libraryDependencies += "com.lihaoyi" %% "upickle" % "3.3.0",
    libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.30",
    libraryDependencies += "com.twilio.sdk" % "twilio" % "7.15.5",

  )
