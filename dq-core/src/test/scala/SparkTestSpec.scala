import com.typesafe.config._
import it.agilelab.bigdata.DataQuality.configs.ConfigReader
import it.agilelab.bigdata.DataQuality.utils.DQSettings
import it.agilelab.bigdata.DataQuality.utils.io.LocalDBManager
import org.apache.hadoop.fs.FileSystem
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}
import org.joda.time
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class SparkTestSpec extends FunSuite with BeforeAndAfterAll {
  @transient private var _sc: SparkContext = _
  def sc: SparkContext = _sc
  @transient private var _fs: FileSystem = _
  def fs: FileSystem = _fs

  val settings: DQSettings = new DQSettings(
    conf = ConfigFactory.parseURL(getClass.getResource("/application.conf")).getConfig("dataquality"),
    configFilePath = getClass.getResource("/conf/usgs-depth.conf").getPath,
    repartition = false,
    local = true,
    ref_date = time.DateTime.now()
  )

  val conf = new SparkConf().setAppName(settings.appName)
    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .set("spark.kryoserializer.buffer.max", "128")
    .set("spark.sql.parquet.compression.codec", "snappy")
    .setMaster("local[*]")

  val localSqlWriter = new LocalDBManager(settings)

  override def beforeAll() {
    _sc = new SparkContext(conf)
    _fs = FileSystem.getLocal(_sc.hadoopConfiguration)
    super.beforeAll()
  }

  test("parse basic conf") {
    val configuration = new ConfigReader(settings.configFilePath)(localSqlWriter, settings)
    println(configuration.sourcesConfigMap)
    assert(true)
  }

  override def afterAll() {
    sc.stop()
    System.clearProperty("spark.driver.port")
    _sc = null
    super.afterAll()
  }
}
