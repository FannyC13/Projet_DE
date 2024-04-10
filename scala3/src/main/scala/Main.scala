import java.time.Instant
import org.json4s.DefaultFormats
import java.nio.file.{Files, Paths}
import scala.io.Source

object Main {
  implicit val formats: DefaultFormats.type = DefaultFormats

  def main(args: Array[String]): Unit = {

    val reportsToWrite = List(
      IOTReport("ID001", Instant.parse("2024-04-07T12:10:00Z"), "Epita est meilleur que l'Efrei", 48.858370,2.294481 ),
      IOTReport("ID002", Instant.parse("2024-04-02T12:10:15Z"), "Je n'en peux plus des partiels",48.858370, 2.294481)
    )
    
    println("Testing readFileCSV:")
    val csvFilePath = "src/ressources/testRead.csv"
    val csvLines = Files.readAllLines(Paths.get(csvFilePath)).toArray.map(_.asInstanceOf[String])
    val csvParsedReports = csvLines.tail.map(_.split(",").toList).map(IOTReport.readFileCSV)
    csvParsedReports.foreach {
      case Right(report) => println(report)
      case Left(error) => println(s"Error: $error")
    }


    println("\nTesting writeFileCSV:")
    val csvOutputFilePath = "src/ressources/testWrite.csv"
    IOTReport.writeFileCSV(reportsToWrite, csvOutputFilePath)
    println(s"CSV data written to $csvOutputFilePath")

  
    println("\nTesting readFileJSON:")
    val jsonFilePath = "src/ressources/testRead.json"
    val jsonParsedReports = IOTReport.readFileJSON(jsonFilePath)
    jsonParsedReports match {
      case Right(reports) => reports.foreach(println)
      case Left(error) => println(s"Error: $error")
    }

  
    println("\nTesting writeFileJSON:")
    val jsonOutputFilePath = "src/ressources/testWrite.json"
    IOTReport.writeFileJSON(reportsToWrite, jsonOutputFilePath)
    println(s"JSON data written to $jsonOutputFilePath")
  }


}

