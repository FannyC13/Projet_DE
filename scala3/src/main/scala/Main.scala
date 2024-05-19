import java.time.Instant
import org.json4s.DefaultFormats
import java.nio.file.{Files, Paths}
import scala.io.Source

object Main {
  

  def main(args: Array[String]): Unit = {

    val reportsToWrite = List(

      IOTReport(22395192,20236189,"CSIG","M2","Republique",48.789184466072136,2.363869590077075, Instant.parse("2024-04-07T12:10:00Z"),"Je vais arriver en retard, je suis coincé dans le trafic."),
      IOTReport(27758898,20230752,"DAI","M2","Republique",48.78883968025621,2.3637500342382443,Instant.parse("2024-04-02T12:10:15Z"),"Tu as déjà essayé le restaurant japonais qui vient d'ouvrir ?")
  
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

