import java.time.{Instant, ZoneId}
import scala.util.Random
import scala.io.Source
object IoTSimulator {
  val random = new Random()

  
  def randomStudentID(): String = {
    s"ID${random.nextInt(1000)}" 
  }

  
  def randomTimestamp(): Instant = {
    val start = Instant.parse("2024-04-01T00:00:00Z")
    val end = Instant.now()
    Instant.ofEpochMilli(start.toEpochMilli + random.nextLong() % (end.toEpochMilli - start.toEpochMilli))
  }

  
  def randomSentence(): String = {
    val filename = "src/ressources/sentences.txt"
    val sentences = Source.fromFile(filename).getLines.toList
    sentences(random.nextInt(sentences.length))
  }

  
  def randomLatitude(): Double = {
    -90 + random.nextDouble() * 180 
  }

  
  def randomLongitude(): Double = {
    -180 + random.nextDouble() * 360 
  }

  
  def generateRandomIoTReport(): IOTReport = {
    val studentID = randomStudentID()
    val timestamp = randomTimestamp()
    val sentence = randomSentence()
    val latitude = randomLatitude()
    val longitude = randomLongitude()
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
    
    val reports = IoTSimulator.generateRandomIoTReports(50)
    
    
    val csvFilePath = "src/ressources/Simulator.json"
    IOTReport.writeFileJSON(reports, csvFilePath) 
  }
}
