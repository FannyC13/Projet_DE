val scala2_13Version = "2.13.10"
val sparkVersion = "3.3.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "scala2.13",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala2_13Version,

    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      "org.scalatest" %% "scalatest" % "3.2.10" % Test,
      "org.json4s" %% "json4s-native" % "4.0.3",
      "com.lihaoyi" %% "upickle" % "3.3.0",
      "org.apache.kafka" % "kafka-clients" % "3.2.0",
      "org.slf4j" % "slf4j-simple" % "1.7.30",
      "com.twilio.sdk" % "twilio" % "8.25.0",
      "org.apache.spark" %% "spark-core" % sparkVersion,
      "org.apache.spark" %% "spark-sql" % sparkVersion
    )
  )
