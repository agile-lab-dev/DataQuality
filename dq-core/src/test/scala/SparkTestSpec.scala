import com.typesafe.config._
import it.agilelab.bigdata.DataQuality.configs.ConfigReader
import it.agilelab.bigdata.DataQuality.metrics.{ColumnMetric, FileMetric, MetricProcessor}
import it.agilelab.bigdata.DataQuality.sources.{HdfsFile, SourceConfig}
import it.agilelab.bigdata.DataQuality.utils.DQSettings
import it.agilelab.bigdata.DataQuality.utils.io.HistoryDBManager
import org.apache.hadoop.fs.FileSystem
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.apache.spark.{SparkConf, SparkContext}
import org.joda.time
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.util.Random

class SparkTestSpec extends FunSuite with BeforeAndAfterAll {
  @transient private var _sc: SparkContext = _
  def sc: SparkContext = _sc
  @transient private var _fs: FileSystem = _
  def fs: FileSystem = _fs

  val SAMPLE_SIZE = 100
  val r = Random
  r.setSeed(123)

  val settings: DQSettings = new DQSettings(
    conf = ConfigFactory.parseURL(getClass.getResource("/application.conf")).getConfig("dataquality"),
    configFilePath = getClass.getResource("/conf/test.conf").getPath,
    repartition = false,
    local = true,
    ref_date = time.DateTime.now()
  )

  val conf = new SparkConf().setAppName(settings.appName)
    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .set("spark.kryoserializer.buffer.max", "128")
    .set("spark.sql.parquet.compression.codec", "snappy")
    .setMaster("local[*]")

  val localSqlWriter = new HistoryDBManager(settings)

  override def beforeAll() {
    _sc = new SparkContext(conf)
    _fs = FileSystem.getLocal(_sc.hadoopConfiguration)
    super.beforeAll()
  }

  test("parse basic conf") {
    val configuration = new ConfigReader(settings.configFilePath)(localSqlWriter, settings)

    val testSource: HdfsFile = HdfsFile("T1", "./t1.csv", "csv", true, "2018-03-26", None)
    val sources: Map[String, SourceConfig] = configuration.sourcesConfigMap
    assert(sources.keySet.size == 3, "Should be equal 3")
    assert(sources.keySet == Set("T1","T2","T3"))
    assert(sources("T1") === testSource)
    assert(true)
  }

  case class TestRow(
                      str: String = r.nextString(5),
                      int: Int = r.nextInt(),
                      long: Long = r.nextLong(),
                      double: Double = r.nextDouble(),
                      boolean: Boolean = r.nextBoolean()
                    )

  test("run basic metrics") {
    val sqlContext = new SQLContext(sc)
    val input: DataFrame = sqlContext.createDataFrame(List.fill(SAMPLE_SIZE)(TestRow.apply()))
    val metric = FileMetric("123","ROW_COUNT","","input","2018-12-12",Map.empty)
    val res: (Map[Seq[String], Map[ColumnMetric, (Double, Option[String])]], Map[FileMetric, (Double, Option[String])]) =
      MetricProcessor.processAllMetrics(input, Seq.empty, Seq(metric), Seq.empty)(settings,sc, sqlContext, fs)
    assert(res._2(metric)._1 == SAMPLE_SIZE)
  }

  override def afterAll() {
    sc.stop()
    System.clearProperty("spark.driver.port")
    _sc = null
    super.afterAll()
  }
}
