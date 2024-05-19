import java.time.Instant
import java.util.Properties
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer, ConsumerRecord}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.slf4j.LoggerFactory
import upickle.default._
import scala.collection.JavaConverters._

// Import the existing IOTReport case class
import IOTReport._

// Define the ProcessedIOTReport case class with the troublesome field
case class ProcessedIOTReport(ID_Student: Int, Latitude: Double, Longitude: Double, Timestamp: Instant, Sentence: String, Troublesome: Boolean)

// Define the implicit ReadWriter for ProcessedIOTReport
implicit val processedIOTReportRW: ReadWriter[ProcessedIOTReport] = macroRW

object Badword_Detector {
  val logger = LoggerFactory.getLogger(this.getClass)
  val wordsToDetect = List("morte", "examen", "école", "thermodynamique") // List of words to detect in the Sentence

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

    
    while (true) {
      val records = consumer.poll(java.time.Duration.ofMillis(1000)).asScala

      records.toList
        .flatMap(record => parseRecord(record, wordsToDetect, outputTopic))
        .map { outputRecord =>
          producer.send(outputRecord)
          logger.info(s"Successfully processed and sent report: ${outputRecord.value()}")
        }
    }
    
    sys.addShutdownHook {
      consumer.close()
      producer.close()
      logger.info("Consumer and producer closed successfully.")
    }
  }

  def parseRecord(record: ConsumerRecord[String, String], wordsToDetect: List[String], outputTopic: String): Option[ProducerRecord[String, String]] = {
    val reportOption = scala.util.Try(read[IOTReport](record.value())).toOption
    reportOption.flatMap { report =>
      val troublesome = wordsToDetect.exists(word => report.Sentence.contains(word))
      if (troublesome) {
        logger.debug(s"Detected troublesome word in sentence: ${report.Sentence}")
      }
      val updatedReport = ProcessedIOTReport(
        report.ID_Student,
        report.Latitude,
        report.Longitude,
        report.Timestamp,
        report.Sentence,
        troublesome
      )
      val json = write[ProcessedIOTReport](updatedReport)
      Some(new ProducerRecord[String, String](outputTopic, record.key(), json))
    }
  }
}
