val scala3Version = "3.3.3"

lazy val root = project
  .in(file("."))
  .settings(
    name := "scala3",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      "org.scalatest" %% "scalatest" % "3.2.10" % Test,
      "org.json4s" %% "json4s-native" % "4.0.3",
      "com.lihaoyi" %% "upickle" % "3.3.0",
      "org.apache.kafka" % "kafka-clients" % "3.2.0",
      "org.slf4j" % "slf4j-simple" % "1.7.30",
      "com.twilio.sdk" % "twilio" % "8.25.0"
    )

  )
