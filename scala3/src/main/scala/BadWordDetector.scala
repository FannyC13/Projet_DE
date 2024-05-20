import java.time.Instant
import java.util.Properties
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer, ConsumerRecord}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.slf4j.LoggerFactory
import upickle.default._
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

// Import the existing IOTReport case class
import IOTReport._

// Define the ProcessedIOTReport case class with the troublesome field
case class ProcessedIOTReport(iot_id: Int, ID_Student: Int, promo: String, annee: String, campus: String, Latitude: Double, Longitude: Double, Timestamp: Instant, Sentence: String, Email: String, Troublesome: Boolean)

// Define the implicit ReadWriter for ProcessedIOTReport
object ProcessedIOTReport {
  implicit val processedIOTReportRW: ReadWriter[ProcessedIOTReport] = macroRW
}

object BadWordDetector {
  val logger = LoggerFactory.getLogger(this.getClass)
  val wordsToDetect = List("morte", "mourir", "Epita") // List of words to detect in the Sentence

  def createKafkaConsumer(kafkaHost: String, groupId: String): KafkaConsumer[String, String] = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaHost)
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest") // Start from the latest message
    new KafkaConsumer[String, String](props)
  }

  def createKafkaProducer(kafkaHost: String): KafkaProducer[String, String] = {
    val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaHost)
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    new KafkaProducer[String, String](props)
  }

  def main(args: Array[String]): Unit = {
    if (args.length != 1) {
      logger.error("Please provide the Kafka host as an argument")
      sys.exit(1)
    }

    val kafkaHost = args(0)
    val consumer = createKafkaConsumer(kafkaHost, "iot-report-processor")
    val producer = createKafkaProducer(kafkaHost)
    val inputTopic = "iot_reports_topic"
    val outputTopic = "processed_iot_reports_topic"

    consumer.subscribe(java.util.Collections.singletonList(inputTopic))

    val runtime = Runtime.getRuntime
    runtime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        logger.info("Shutting down consumer and producer...")
        consumer.close()
        producer.close()
        logger.info("Consumer and producer closed successfully.")
      }
    })

    try {
      while (true) {
        val records = consumer.poll(java.time.Duration.ofMillis(1000)).asScala

        records.toList
          .flatMap(record => parseRecord(record, wordsToDetect, outputTopic))
          .foreach { outputRecord =>
            logger.info(s"Sending record: ${outputRecord.value()} to $outputTopic")
            producer.send(outputRecord, (metadata, exception) => {
              if (exception != null) {
                logger.error(s"Failed to send record to $outputTopic", exception)
              } else {
                logger.info(s"Successfully sent report: ${outputRecord.value()} to partition ${metadata.partition()} with offset ${metadata.offset()}")
              }
            })
          }
      }
    } catch {
      case e: Exception =>
        logger.error("Error while processing records", e)
    } finally {
      consumer.close()
      producer.close()
      logger.info("Consumer and producer closed successfully.")
    }
  }

  def parseRecord(record: ConsumerRecord[String, String], wordsToDetect: List[String], outputTopic: String): Option[ProducerRecord[String, String]] = {
    val reportOption = Try(read[IOTReport](record.value())) match {
      case Success(report) =>
        logger.info(s"Read record: ${record.value()}")
        Some(report)
      case Failure(exception) =>
        logger.error(s"Failed to parse record: ${record.value()}", exception)
        None
    }

    reportOption.flatMap { report =>
      val troublesome = wordsToDetect.exists(word => report.Sentence.contains(word))
      if (troublesome) {
        logger.debug(s"Detected troublesome word in sentence: ${report.Sentence}")
      }
      val updatedReport = ProcessedIOTReport(
        report.iot_id,
        report.ID_Student,
        report.promo,
        report.annee,
        report.campus,
        report.Latitude,
        report.Longitude,
        report.Timestamp,
        report.Sentence,
        report.Email,
        troublesome
      )
      val json = write[ProcessedIOTReport](updatedReport)
      Some(new ProducerRecord[String, String](outputTopic, record.key(), json))
    }
  }
}
