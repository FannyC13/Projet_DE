import java.nio.file.{Files, Paths}
import java.io.{BufferedWriter, FileWriter}
import scala.util.{Try, Success, Failure}
import org.json4s._
import org.json4s.native.JsonMethods._
import java.time.Instant
import org.json4s.DefaultFormats
import upickle.default.*

// Définition implicite d'un ReadWriter pour les objets Instant
implicit val instantRw: ReadWriter[java.time.Instant] = readwriter[String].bimap(_.toString, java.time.Instant.parse)

// Définition de la classe IOTReport représentant un rapport IoT avec des champs ID_Student, Timestamp, Sentence, Latitude et Longitude
case class IOTReport(ID_Student: String, Timestamp: Instant, Sentence: String, Latitude: Double, Longitude: Double) derives ReadWriter

// Définition implicite d'un ReadWriter pour la classe IOTReport pour sérialiser et désérialiser les objets IOTReport
implicit val ownerRw: ReadWriter[IOTReport] = macroRW[IOTReport]

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
        case Failure(e) => Left(s"Error parsing CSV: ${e.getMessage}")
      }
    }
  }

  def writeFileCSV(reports: List[IOTReport], filePath: String): Unit = {
    val header = "ID_Student,Timestamp,Sentence,Latitude,Longitude"
    val content = reports
      .map(report =>
        s"${report.ID_Student},${report.Timestamp},${report.Sentence},${report.Latitude},${report.Longitude}"
      )
      .mkString("\n")
    val result = Try {
      val writer = new BufferedWriter(new FileWriter(filePath))
      writer.write(s"$header\n$content")
      writer.close()
    }

    result match {
      case Failure(e) => println(s"Error writing CSV: ${e.getMessage}")
      case _ =>
    }
  }

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

  def writeFileJSON(reports: List[IOTReport], filePath: String): Unit = {
    val json : String = write(reports)
    val result = Try {
      val writer = new BufferedWriter(new FileWriter(filePath))
      writer.write(json)
      writer.close()
    }

    result match {
      case Failure(e) => println(s"Error writing JSON: ${e.getMessage}")
      case _ =>
    }
  }
}
