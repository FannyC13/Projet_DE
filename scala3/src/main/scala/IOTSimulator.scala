import java.time.{Instant, ZoneId}
import scala.util.Random
import scala.io.Source
object IoTSimulator {
  val random = new Random()

  
  def randomStudentID(): String = {
    s"ID${random.nextInt(1000)}" 
  }

  
  def randomTimestamp(): Instant = {
    val start = Instant.parse("2024-05-02T00:00:00Z")
    val end = Instant.now()
    Instant.ofEpochMilli(start.toEpochMilli + random.nextLong() % (end.toEpochMilli - start.toEpochMilli))
  }

  //Selection de phrases de manière random venant d'un fichier txt
  def randomSentence(): String = {
    val filename = "src/ressources/sentences.txt"
    val sentences = Source.fromFile(filename).getLines.toList
    sentences(random.nextInt(sentences.length))
  }

  // Tracer un cercle correspondant à la zone de chaque campus
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

  
  
  def generateRandomIoTReport(): IOTReport = {
    val zones = List(
      // Campus 1 : Repu
      (48.78878589425504,2.363706878543752, 0.001), 
      // Campus 2 : Gorki
      (48.79005112012818,2.36837788800889, 0.001),
      // Campus 3 :  Home
      (48.789622247614574,2.3692563844627523, 0.001)
    )

    val (centerLat, centerLong, radius) = zones(random.nextInt(zones.length))

    val (latitude, longitude) = randomCoordinatesInCircle(centerLat, centerLong, radius)

    val studentID = randomStudentID()
    val timestamp = randomTimestamp()
    val sentence = randomSentence()
    
    IOTReport(studentID, timestamp, sentence, latitude, longitude)
  }


  def generateRandomIoTReports(count: Int): List[IOTReport] = {
    (1 to count).map(_ => generateRandomIoTReport()).toList
  }

  def parseReport(reportString: String): IOTReport = {
    val fields = reportString.split(",")
    val idStudent = fields(0)
    val timestamp = Instant.parse(fields(1))
    val sentence = fields(2)
    val lat = fields(3).toDouble
    val long = fields(4).toDouble
    IOTReport(idStudent, timestamp, sentence, lat, long)
  }

  def main(args: Array[String]): Unit = {
    
  
    val reports = IoTSimulator.generateRandomIoTReports(100)
    
    
    val jsonFilePath = "src/ressources/SimulatorFile.json"
    IOTReport.writeFileJSON(reports, jsonFilePath) 
  }
}


