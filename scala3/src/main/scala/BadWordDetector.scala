import java.time.Instant
import org.apache.spark.sql.{SparkSession, Dataset, Row}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger
import upickle.default._
import org.slf4j.LoggerFactory
import scala.util.{Try, Success, Failure}

import IOTReport._

case class ProcessedIOTReport(
  iot_id: Int,
  ID_Student: Int,
  promo: String,
  annee: String,
  campus: String,
  Latitude: Double,
  Longitude: Double,
  Timestamp: Instant,
  Sentence: String,
  Troublesome: Boolean
)

object BadWordDetector {
  val logger = LoggerFactory.getLogger(this.getClass)
  val wordsToDetect = List("morte", "examen", "école", "thermodynamique")

  implicit val instantRw: ReadWriter[Instant] = readwriter[String].bimap(_.toString, Instant.parse)
  implicit val iotReportRW: ReadWriter[IOTReport] = macroRW[IOTReport]
  implicit val processedIOTReportRW: ReadWriter[ProcessedIOTReport] = macroRW[ProcessedIOTReport]

  def main(args: Array[String]): Unit = {
    if (args.length != 1) {
      logger.error("Please provide the Kafka bootstrap servers as an argument")
      sys.exit(1)
    }

    val kafkaBootstrapServers = args(0)
    val inputTopic = "iot_reports_topic"
    val outputTopic = "processed_iot_reports_topic"

    val spark = SparkSession.builder()
      .appName("BadwordDetectorSpark")
      .master("local[*]") // Use local[*] to leverage all available cores
      .getOrCreate()

    import spark.implicits._

    // Read data from Kafka
    val rawStream = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaBootstrapServers)
      .option("subscribe", inputTopic)
      .option("startingOffsets", "latest")
      .option("failOnDataLoss", "false") // Add this option to handle data loss gracefully
      .load()

    val iotReports = rawStream.selectExpr("CAST(value AS STRING)").as[String]
      .flatMap(value => Try(read[IOTReport](value)).toOption)

    // Process the data
    val processedReports = iotReports.map { report =>
      val troublesome = wordsToDetect.exists(word => report.Sentence.contains(word))
      ProcessedIOTReport(
        report.iot_id,
        report.ID_Student,
        report.promo,
        report.annee,
        report.campus,
        report.Latitude,
        report.Longitude,
        report.Timestamp,
        report.Sentence,
        troublesome
      )
    }

    // Convert to DataFrame
    val processedReportsDF = processedReports.map(report => (report.ID_Student.toString, write(report))).toDF("key", "value")

    // Write the processed data back to Kafka
    val query = processedReportsDF
      .writeStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaBootstrapServers)
      .option("topic", outputTopic)
      .option("checkpointLocation", "/tmp/spark-checkpoint")
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .start()

    query.awaitTermination()
  }
}
