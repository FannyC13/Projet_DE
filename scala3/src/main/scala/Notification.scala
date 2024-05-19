import java.time.Instant
import java.util.Properties
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer, ConsumerRecord}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.slf4j.LoggerFactory
import upickle.default._
import scala.collection.JavaConverters._
import com.twilio.Twilio
import com.twilio.`type`.PhoneNumber
import com.twilio.rest.api.v2010.account.Message

// Import the existing IOTReport case class
import Badword_Detector._

object Notification {
  val logger = LoggerFactory.getLogger(this.getClass)


  val ACCOUNT_SID = sys.env("TWILIO_ACCOUNT_SID")
  val AUTH_TOKEN = sys.env("TWILIO_AUTH_TOKEN")
  val FROM_PHONE_NUMBER = sys.env("TWILIO_FROM_PHONE_NUMBER")

  if (ACCOUNT_SID == null || AUTH_TOKEN == null || FROM_PHONE_NUMBER == null) {
    logger.error("Twilio credentials and phonels  number must be set in environment variables")
    sys.exit(1)
  }

  Twilio.init(ACCOUNT_SID, AUTH_TOKEN)

  def createKafkaConsumer(kafkaHost: String, groupId: String): KafkaConsumer[String, String] = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaHost)
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    new KafkaConsumer[String, String](props)
  }

  def detectTroublesomeWord(sentence: String, wordsToDetect: List[String]): Option[String] = {
    wordsToDetect.find(word => sentence.contains(word))
  }

  def sendSMS(to: String, message: String): Unit = {
    Message.creator(
      new PhoneNumber(to),
      new PhoneNumber(FROM_PHONE_NUMBER),
      message
    ).create()
  }

  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      logger.error("Veuillez mettre le host et le numéro de téléphone")
      sys.exit(1)
    }

    val kafkaHost = args(0)
    val recipientPhoneNumber = args(1)
    val inputTopic = "processed_iot_reports_topic"
    val consumer = createKafkaConsumer(kafkaHost, "troublesome-message-printer")

    consumer.subscribe(java.util.Collections.singletonList(inputTopic))

    while (true) {
      val records = consumer.poll(java.time.Duration.ofMillis(1000)).asScala

      records.toList
        .flatMap(record => parseRecord(record))
        .map { message =>
          sendSMS(recipientPhoneNumber, message)
          logger.info(s"Sent SMS: $message")
        }
    }

    sys.addShutdownHook {
      consumer.close()
      logger.info("Consumer closed successfully.")
    }
  }

  def parseRecord(record: ConsumerRecord[String, String]): Option[String] = {
    val processedReportOption = scala.util.Try(read[ProcessedIOTReport](record.value())).toOption
    processedReportOption.flatMap { processedReport =>
      if (processedReport.Troublesome) {
        val troublesomeWord = detectTroublesomeWord(processedReport.Sentence, wordsToDetect).getOrElse("unknown word")
        Some(s"ID_Etudiant: ${processedReport.ID_Student} \n Detected Word: $troublesomeWord \n Message : '${processedReport.Sentence}' \n Localisation : (${processedReport.Latitude}, ${processedReport.Longitude}) \n Heure : ${processedReport.Timestamp} ")
      } else {
        None
      }
    }
  }
}
