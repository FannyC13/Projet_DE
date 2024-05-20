import java.time.{Instant, ZoneId, LocalDateTime, LocalDate, LocalTime}
import scala.util.Random
import scala.io.Source
import java.util.Properties
import upickle.default._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, ProducerConfig}
import org.slf4j.LoggerFactory

object IOTSimulator {
  val logger = LoggerFactory.getLogger(this.getClass)
  val random = new Random()

  // Génère un ID étudiant aléatoire
  def randomStudentID(): Int = {
    Random.between(20200000, 20280000)
  }

  // Génère un ID IoT aléatoire
  def randomIotID(): Int = {
    Random.between(20000000, 30000000)
  }

  // Sélectionne une année aléatoire parmi une liste prédéfinie
  def randomAnnee() : String = {
    val annees = Array("M1", "M2", "M2PRO")
    annees(Random.nextInt(annees.length))
  }

  // Sélectionne une promotion aléatoire parmi une liste prédéfinie
  def randomPromo() : String = {
    val promos = Array("BDML", "LSI", "RS", "DAI", "DE", "BIA", "SWI", "TI", "ITF", "CIL", "CSIG", "CC", "SRD")
    promos(Random.nextInt(promos.length))
  }

  def randomEmail(): String = {
    val domains = Array("gmail.com", "yahoo.com", "hotmail.com", "example.com")
    val prefix = Random.alphanumeric.take(10).mkString
    s"$prefix@${domains(Random.nextInt(domains.length))}"
  }

  // Génère un timestamp aléatoire avec 95% de chances pendant la journée et 5% pendant la nuit
  def randomTimestamp(): Instant = {
    val startOf2024 = LocalDateTime.of(2024, 1, 1, 0, 0).atZone(ZoneId.systemDefault()).toEpochSecond
    val endOf2024 = LocalDateTime.of(2024, 12, 31, 23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond

    val randomTime = if (Random.nextDouble() < 0.95) {
      val hour = Random.between(7, 22) // 7 to 21 inclusive
      LocalTime.of(hour, Random.nextInt(60), Random.nextInt(60))
    } else {
      val hour = if (Random.nextBoolean()) Random.nextInt(7) else Random.between(21, 24)
      LocalTime.of(hour, Random.nextInt(60), Random.nextInt(60))
    }

    val randomDay = Random.between(startOf2024, endOf2024)
    val randomDateTime = LocalDate.ofEpochDay(randomDay / 86400).atTime(randomTime)
    randomDateTime.atZone(ZoneId.systemDefault()).toInstant
  }

  // Sélectionne une phrase aléatoire provenant d'un fichier txt
  def randomSentence(): String = {
    val filename = "src/ressources/sentences.txt"
    val sentences = Source.fromFile(filename).getLines.toList
    sentences(random.nextInt(sentences.length))
  }

  // Génère des coordonnées aléatoires dans un cercle autour d'un centre donné
  def randomCoordinatesInCircle(centerLat: Double, centerLong: Double, radius: Double): (Double, Double) = {
    val randomRadius = radius * math.sqrt(random.nextDouble())
    val randomAngle = random.nextDouble() * 2 * math.Pi

    // Décalage des coordonnées par rapport au centre du cercle
    val latOffset = randomRadius * math.cos(randomAngle) / 111000.0 
    val longOffset = randomRadius * math.sin(randomAngle) / (111000.0 * math.cos(math.toRadians(centerLat))) 

    // Nouvelles coordonnées à l'intérieur du cercle
    val newLat = centerLat + latOffset
    val newLong = centerLong + longOffset

    (newLat, newLong)
  }
  
  // Génère un rapport IoT aléatoire
  def generateRandomIoTReport(): IOTReport = {
    val zones = List(
      // Campus 1 : Repu
      (48.78878589425504,2.363706878543752, 0.001, "Republique"), 
      // Campus 2 : Gorki
      (48.79005112012818,2.36837788800889, 0.001, "Gorki"),
      // Campus 3 :  Home
      (48.789622247614574,2.3692563844627523, 0.001, "Home")
    )

    val (centerLat, centerLong, radius, campus) = zones(random.nextInt(zones.length))
    
    val (latitude, longitude) = randomPosition(campus) // randomCoordinatesInCircle(centerLat, centerLong, radius)

    val studentID = randomStudentID()
    val iotID = randomIotID()
    val annee = randomAnnee()
    val promo = randomPromo()
    val timestamp = randomTimestamp()
    val sentence = randomSentence()
    val email = if (sentence.contains("Epita") || sentence.contains("morte") || sentence.contains("mourir")) {
      "fchang1311@gmail.com"
    } else {
      randomEmail()
    }
    
    IOTReport(iotID, studentID, promo, annee, campus, latitude, longitude, timestamp, sentence,email)
  }

  // Génère une liste de rapports IoT aléatoires
  def generateRandomIoTReports(count: Int): List[IOTReport] = {
    (1 to count).map(_ => generateRandomIoTReport()).toList
  }

  // Analyse une ligne CSV pour créer un rapport IoT
  def parseReport(reportString: String): IOTReport = {
    val fields = reportString.split(",")
    val iotId = fields(0).toInt
    val idStudent = fields(1).toInt
    val promo = fields(2)
    val annee = fields(3)
    val campus = fields(4)
    val lat = fields(5).toDouble
    val long = fields(6).toDouble
    val timestamp = Instant.parse(fields(7))
    val sentence = fields(8).replace("\"", "")
    val email = fields(9)

    IOTReport(iotId, idStudent, promo, annee, campus, lat, long, timestamp, sentence, email)
  }

  // Crée un producteur Kafka avec des configurations par défaut
  def createKafkaProducer(): KafkaProducer[String, String] = {
    val props = new Properties()
    props.put("bootstrap.servers", "172.17.0.2:9092")  // Explicitly set here for clarity
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    new KafkaProducer[String, String](props)
  }

  // Crée un producteur Kafka avec un hôte spécifié
  def createKafkaProducer(kafkaHost: String): KafkaProducer[String, String] = {
    val props = new Properties()
    props.put("bootstrap.servers", kafkaHost)  // Explicitly set here for clarity
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    new KafkaProducer[String, String](props)
  }
  

  def randomPosition(campus: String): (Double, Double) = {
    val campuses = Map(
    "Republique" -> ((48.788806, 48.790083), (2.363417, 2.364028)),
    "Gorki" -> ((48.789750, 48.790389), (2.367750, 2.368722)),
    "Home" -> ((48.789500, 48.789806), (2.369083, 2.369389))
    )
    val ((latMin, latMax), (lonMin, lonMax)) = campuses(campus)
    val latitude = latMin + (latMax - latMin) * Random.nextDouble()
    val longitude = lonMin + (lonMax - lonMin) * Random.nextDouble()
    (latitude, longitude)
  }
  def main(args: Array[String]): Unit = {
    if (args.length != 1) {
      logger.error("Please provide the Kafka host as an argument")
      sys.exit(1)
    }
    
    val host = args(0) // Retrieve the Kafka host from the command-line arguments
    val reports = generateRandomIoTReports(500)
    val producer = createKafkaProducer(host)
    val topic = "iot_reports_topic"  // Ensure this topic is created in Kafka

    reports.foreach { report =>
      val json = write(report)
      val record = new ProducerRecord[String, String](topic, report.ID_Student.toString, json)
      producer.send(record)
      logger.info(s"Successfully sent report: $json")
    }

    producer.close()
    logger.info("Producer closed successfully.")
  }
}