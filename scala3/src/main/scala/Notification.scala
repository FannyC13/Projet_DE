import org.apache.spark.sql.{SparkSession, DataFrame}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.streaming.Trigger
import org.slf4j.LoggerFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.messaging.{FirebaseMessaging, Message}
import java.io.FileInputStream
import java.time.Instant
import java.util.HashMap
import BadWordDetector._

object Notification {
  val logger = LoggerFactory.getLogger(this.getClass)

  // Remplacez par le chemin vers votre fichier de clé privée Firebase
  val serviceAccount = new FileInputStream("src/ressources/my-efrei-detector-y4u29v-firebase-adminsdk-q75hj-373e24ccae.json")

  val options = FirebaseOptions.builder()
    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
    .build()

  FirebaseApp.initializeApp(options)

  val db = FirestoreClient.getFirestore()

  def detectTroublesomeWord(sentence: String, wordsToDetect: List[String]): Option[String] = {
    wordsToDetect.find(word => sentence.contains(word))
  }

  def saveToFirestore(collection: String, data: Map[String, Any]): Unit = {
    val docRef = db.collection(collection).document()
    val convertedData = new HashMap[String, Any]()
    data.foreach { case (key, value) => 
      convertedData.put(key, value match {
        case v: String => v
        case v: Int => v: java.lang.Integer
        case v: Double => v: java.lang.Double
        case _ => value.toString
      })
    }
    docRef.set(convertedData).get()
    logger.info(s"Saved document to Firestore: $data")

    val message = Message.builder()
    .putData("title", "Alerte Mauvaise conduite détectée!")
    .putData("body", "Des nouveaux messages ont été détectés.")
    .setTopic("all_devices")
    .build()

    FirebaseMessaging.getInstance().send(message)
    logger.info("Notification envoyée à FCM.")
  }

  def sendWarningEmail(email: String, sentence: String): Unit = {
    val subject = "Attention Mauvaise conduite détectée !"
    val content = s"Une mauvaise conduite a été détectée dans le message suivant : $sentence"
    EmailUtil.sendEmail(email, subject, content)
  }

  def main(args: Array[String]): Unit = {
    if (args.length != 1) {
      logger.error("Please provide the Kafka host as an argument")
      sys.exit(1)
    }

    val kafkaHost = args(0)
    val inputTopic = "processed_iot_reports_topic"

    val spark = SparkSession.builder
      .appName("Notification")
      .master("local[*]") // Change this for production
      .getOrCreate()

    import spark.implicits._

    // Define the schema for ProcessedIOTReport
    val schema = new StructType().add("ID_Student", IntegerType).add("Latitude", DoubleType).add("Longitude", DoubleType)
      .add("Timestamp", StringType).add("Sentence", StringType).add("Email", StringType).add("Troublesome", BooleanType)

    val kafkaDF = spark.readStream.format("kafka").option("kafka.bootstrap.servers", kafkaHost).option("subscribe", inputTopic)
      .option("startingOffsets", "latest")
      .load()

    val processedIOTReportsDF = kafkaDF.selectExpr("CAST(value AS STRING) AS json")
      .select(from_json($"json", schema).as("data"))
      .select("data.*")

    val troublesomeReportsDF = processedIOTReportsDF.filter($"Troublesome")
      .map { row =>
        val ID_Student = row.getInt(0)
        val Latitude = row.getDouble(1)
        val Longitude = row.getDouble(2)
        val Timestamp = row.getString(3)
        val Sentence = row.getString(4)
        val Email = row.getString(5)
        val troublesomeWord = detectTroublesomeWord(Sentence, wordsToDetect).getOrElse("")
        val localisation = s"($Latitude, $Longitude)"
        
        

        logger.info(s"Row details: ID_Student=$ID_Student, Latitude=$Latitude, Longitude=$Longitude, Timestamp=$Timestamp, Sentence=$Sentence, Email=$Email, troublesomeWord=$troublesomeWord")

        (troublesomeWord, localisation, Sentence, ID_Student, Timestamp,Email)
      }.toDF("BadWord", "Localisation", "Message", "Student_ID", "Timestamp", "Email")

    val query = troublesomeReportsDF.writeStream
      .outputMode("append")
      .foreachBatch { (batchDF: DataFrame, batchId: Long) =>
        batchDF.as[(String, String, String, Int, String,String)].collect().foreach { case (badWord, localisation, message, studentId, timestamp,email) =>
          val data = Map(
            "BadWord" -> badWord,
            "Localisation" -> localisation,
            "Message" -> message,
            "Student_ID" -> studentId,
            "Timestamp" -> timestamp
          )
          logger.info(s"Saving data to Firestore: $data")
          saveToFirestore("badwords", data)
          sendWarningEmail(email, message)
        }
      }
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .start()

    query.awaitTermination()
  }
}
