import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{StructType, IntegerType, DoubleType, StringType, BooleanType}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger
import org.slf4j.LoggerFactory

object HDFSWriter {
  val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    if (args.length != 1) {
      logger.error("Please provide the Kafka host as an argument")
      sys.exit(1)
    }

    val kafkaHost = args(0)
    val inputTopic = "processed_iot_reports_topic"
    val hdfsPath = "hdfs://localhost:8020/user/hdfs/processed_reports"

    val spark = SparkSession.builder
      .appName("HDFSWriter")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    val schema = new StructType()
      .add("ID_Student", IntegerType)
      .add("Latitude", DoubleType)
      .add("Longitude", DoubleType)
      .add("Timestamp", StringType)
      .add("Sentence", StringType)
      .add("Troublesome", BooleanType)

    val kafkaDF = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaHost)
      .option("subscribe", inputTopic)
      .option("startingOffsets", "latest")
      .load()

    val processedIOTReportsDF = kafkaDF.selectExpr("CAST(value AS STRING) AS json")
      .select(from_json($"json", schema).as("data"))
      .select("data.*")

    val troublesomeReportsDF = processedIOTReportsDF.filter($"Troublesome")

    val query = troublesomeReportsDF.writeStream
      .outputMode("append")
      .format("parquet")
      .option("path", hdfsPath)
      .option("checkpointLocation", "hdfs://localhost:8020/user/hdfs/checkpoint")
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .start()

    query.awaitTermination()
  }
}
