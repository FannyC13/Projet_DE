import java.nio.file.{Files, Paths}
import java.io.{BufferedWriter, FileWriter}
import scala.util.{Try, Success, Failure}
import org.json4s._
import org.json4s.native.JsonMethods._
import java.time.Instant

case class IOTReport(ID_Student: String, Timestamp: Instant, Sentence: String, Lat : Double, Long : Double) 

object IOTReport {
  def readFileCSV(csv: List[String]): Either[String, IOTReport] = {
    if (csv.length != 5) {
      Left("Invalid CSV format.")
    } else {
      val result = Try {
        val id = csv(0)
        val timestamp = Instant.parse(csv(1))
        val sentence = csv(2)
        val lat = csv(3).toDouble
        val long = csv(4).toDouble
        IOTReport(id, timestamp, sentence, lat, long)
      }

      result match {
        case Success(report) => Right(report)
        case Failure(e)      => Left(s"Error parsing CSV: ${e.getMessage}")
      }
    }
  }

  def writeFileCSV(reports: List[IOTReport], filePath: String): Unit = {
    val header = "ID_Student,Timestamp,Sentence,Latitude,Longitude"
    val content = reports
      .map(report =>
        s"${report.ID_Student},${report.Timestamp},${report.Sentence},${report.Lat},${report.Long}"
      )
      .mkString("\n")
    val writer = new BufferedWriter(new FileWriter(filePath))
    try { writer.write(s"$header\n$content") }
    finally {
      writer.close()
    }
  }

  def readFileJSON(
      filePath: String
  )(implicit formats: Formats): Either[String, List[IOTReport]] = {
    val result = Try {
      val jsonString = new String(Files.readAllBytes(Paths.get(filePath)))
      val json = parse(jsonString)
      val reports = json.extract[List[Map[String, String]]]
      reports.map { report =>
        val id = report("ID_Student")
        val timestamp = Instant.parse(report("Timestamp"))
        val sentence = report("Sentence")
        val lat = report("Longitude").toDouble
        val long = report("Latitude").toDouble
        IOTReport(id, timestamp, sentence, lat, long)
      }
    }
    result match {
      case Success(reports) => Right(reports)
      case Failure(e)       => Left(s"Error reading JSON file: ${e.getMessage}")
    }
  }

  def writeFileJSON(reports: List[IOTReport], filePath: String)(implicit
      formats: Formats
  ): Unit = {
    val jsonReports = reports.map { report =>
      JObject(
        "ID_Student" -> JString(report.ID_Student),
        "Timestamp" -> JString(report.Timestamp.toString),
        "Sentence" -> JString(report.Sentence),
        "Latitude" -> JString(report.Lat.toString),
        "Longitude" -> JString(report.Long.toString)
      )
    }

    val json = JArray(jsonReports)

    val writer = new BufferedWriter(new FileWriter(filePath))
    try {
      writer.write(pretty(render(json)))
    } finally {
      writer.close()
    }
  }
}
