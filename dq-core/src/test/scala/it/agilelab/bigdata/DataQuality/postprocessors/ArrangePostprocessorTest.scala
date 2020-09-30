package it.agilelab.bigdata.DataQuality.postprocessors

import com.typesafe.config._
import it.agilelab.bigdata.DataQuality.configs.ConfigReader
import it.agilelab.bigdata.DataQuality.utils.DQSettings
import it.agilelab.bigdata.DataQuality.utils.io.HistoryDBManager
import org.apache.spark.sql._
import org.apache.spark.SparkConf
import org.joda.time
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class ArrangePostprocessorTest extends FunSuite with BeforeAndAfterAll {
  implicit val settings: DQSettings = new DQSettings(
    conf = ConfigFactory.parseURL(getClass.getResource("/application.conf")).getConfig("data_quality"),
    configFilePath = getClass.getResource("/conf/number_precision.conf").getPath,
    repartition = false,
    local = true,
    ref_date = time.DateTime.now()
  )

  implicit val sqlWriter = new HistoryDBManager(settings)

  val config: ConfigReader = new ConfigReader(settings.configFilePath)

  val conf = new SparkConf().setAppName(settings.appName).setMaster("local[*]")

  override def beforeAll() {
    super.beforeAll()
  }

  val spark = SparkSession.builder.config(conf).getOrCreate()
  import spark.implicits._

  val processor: ArrangePostprocessor = config.getPostprocessors.head.asInstanceOf[ArrangePostprocessor]

  implicit val df : DataFrame = Seq(("john", "black", "1992", "87.2", "50", "1.2842922E2")).toDF("name", "surname", "year", "weigth", "age", "amount")

  val result: Row = df.select(processor.columns.map(_.toColumn): _*).collect().head

  test("arrange, column String, return the same String") {
    assert(result.get(0).isInstanceOf[String])
    assert(result.get(0).equals("john"))
  }

  test("arrange, column String, return a formatted String (prefix Hello)") {
    assert(result.get(1).isInstanceOf[String])
    assert(result.get(1).equals("Hello black"))
  }

  test("arrange, column String, return an Integer column") {
    assert(result.get(2).isInstanceOf[Integer])
    assert(result.get(2).equals(1992))
  }

  test("arrange, column String, return a Double column") {
    assert(result.get(3).isInstanceOf[Double])
    assert(result.get(3).equals(87.2))
  }

  test("arrange, column String, return a String column with a cast to Double with precision equals to 2") {
    assert(result.get(4).isInstanceOf[String])
    assert(result.get(4).equals("50.00"))
  }

  test("arrange, column String, return a String column with a formatted String (numeric format)") {
    assert (result.get(5).isInstanceOf[String])
    assert (result.get(5).equals("128.43"))
  }

  override def afterAll() {
    spark.stop()
    super.afterAll()
  }
}
