import java.nio.file.{Files, Paths}
import java.io.{BufferedWriter, FileWriter}
import scala.util.{Try, Success, Failure}
import java.time.Instant
import scala.util.Random

import upickle.default._

// Définition implicite d'un ReadWriter pour les objets Instant
implicit val instantRw: ReadWriter[Instant] = readwriter[String].bimap(_.toString, Instant.parse)

// Définition de la classe IOTReport représentant un rapport IoT avec des champs iot_id, ID_Student, promo, annee, campus, Latitude, Longitude, Timestamp et Sentence
case class IOTReport(iot_id: Int, ID_Student: Int, promo: String, annee: String, campus: String, Latitude: Double, Longitude: Double, Timestamp: Instant, Sentence: String) derives ReadWriter

// Définition implicite d'un ReadWriter pour la classe IOTReport pour sérialiser et désérialiser les objets IOTReport
implicit val ownerRw: ReadWriter[IOTReport] = macroRW[IOTReport]

object IOTReport {
  // Méthode pour lire un rapport IoT à partir d'une ligne CSV
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
        IOTReport(iot_id, student_id, promo, annee, campus, lat, long, timest, message)
      }

      result match {
        case Success(report) => Right(report)
        case Failure(e) => Left(s"Error parsing CSV: ${e.getMessage}")
      }
    }
  }

  // Méthode pour écrire une liste de rapports IoT dans un fichier CSV
  def writeFileCSV(reports: List[IOTReport], filePath: String): Unit = {
    val header = "IOT_ID,Student_ID,Promo,Année,Campus,Latitude,Longitude,Timestamp,Text\n"
    val content = reports
      .map(report =>
        s"${report.iot_id},${report.ID_Student},${report.promo},${report.annee},${report.campus},${report.Latitude},${report.Longitude},${report.Timestamp},\"${report.Sentence}\""
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

  // Méthode pour lire une liste de rapports IoT à partir d'un fichier JSON
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

  // Méthode pour écrire une liste de rapports IoT dans un fichier JSON
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