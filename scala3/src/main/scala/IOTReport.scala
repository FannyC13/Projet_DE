import java.time.Instant
import java.nio.file.{Files, Paths}
import java.io.{BufferedWriter, FileWriter}
import scala.util.{Try, Success, Failure}
import upickle.default._

case class IOTReport(
  iot_id: Int,
  ID_Student: Int,
  promo: String,
  annee: String,
  campus: String,
  Latitude: Double,
  Longitude: Double,
  Timestamp: Instant,
  Sentence: String,
  Email : String
)

object IOTReport {
  implicit val instantRw: ReadWriter[Instant] = readwriter[String].bimap(_.toString, Instant.parse)
  implicit val iotReportRW: ReadWriter[IOTReport] = macroRW[IOTReport]

  // Method to read an IoT report from a CSV line
  def readFileCSV(csv: List[String]): Either[String, IOTReport] = {
    if (csv.length != 9) {
      Left("Invalid CSV format.")
    } else {
      val result = Try {
        val iot_id = csv(0).toInt
        val student_id = csv(1).toInt
        val promo = csv(2)
        val annee = csv(3)
        val campus = csv(4)
        val lat = csv(5).toDouble
        val long = csv(6).toDouble
        val timest = Instant.parse(csv(7))
        val message = csv(8)
        val mail = csv(9)
        IOTReport(iot_id, student_id, promo, annee, campus, lat, long, timest, message,mail)
      }

      result match {
        case Success(report) => Right(report)
        case Failure(e) => Left(s"Error parsing CSV: ${e.getMessage}")
      }
    }
  }

  // Method to write a list of IoT reports to a CSV file
  def writeFileCSV(reports: List[IOTReport], filePath: String): Unit = {
    val header = "IOT_ID,Student_ID,Promo,Année,Campus,Latitude,Longitude,Timestamp,Text,Mail\n"
    val content = reports
      .map(report =>
        s"${report.iot_id},${report.ID_Student},${report.promo},${report.annee},${report.campus},${report.Latitude},${report.Longitude},${report.Timestamp},\"${report.Sentence}\", ${report.Email}"
      )
      .mkString("\n")
    val result = Try {
      val writer = new BufferedWriter(new FileWriter(filePath))
      writer.write(s"$header$content")
      writer.close()
    }
    result match {
      case Success(_) => println(s"File written successfully to $filePath")
      case Failure(e) => println(s"Error writing file: ${e.getMessage}")
    }
  }

  // Method to read a list of IoT reports from a JSON file
  def readFileJSON(filePath: String): Either[String, List[IOTReport]] = {
    val result = Try {
      val jsonString = new String(Files.readAllBytes(Paths.get(filePath)))
      val reports = read[List[IOTReport]](jsonString)
      reports
    }
    result match {
      case Success(reports) => Right(reports)
      case Failure(e) => Left(s"Error reading JSON file: ${e.getMessage}")
    }
  }

  // Method to write a list of IoT reports to a JSON file
  def writeFileJSON(reports: List[IOTReport], filePath: String): Unit = {
    val json: String = write(reports)
    val result = Try {
      val writer = new BufferedWriter(new FileWriter(filePath))
      writer.write(json)
      writer.close()
    }
    result match {
      case Failure(e) => println(s"Error writing JSON: ${e.getMessage}")
      case _ => println(s"File written successfully to $filePath")
    }
  }
}
