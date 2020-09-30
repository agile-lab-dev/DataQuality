package it.agilelab.bigdata.DataQuality.apps

import org.apache.log4j.Logger
import org.scalatest.{BeforeAndAfterAll, FunSuite, Tag}

import scala.io.Source

class DQMasterBranchTest extends FunSuite with BeforeAndAfterAll {

  @transient lazy val log: Logger = Logger.getLogger(getClass.getName)

  object Spark1xTest extends Tag("it.agilelab.bigdata.DataQuality.Spark1xTest")
  object Spark2xTest extends Tag("it.agilelab.bigdata.DataQuality.Spark2xTest")

  val dir : String = Option(System.getenv("CI_PROJECT_DIR")).getOrElse("./")

  val outputFileName : String = s"$dir/tmp/checks/CHECKS.csv"

  test("must be run with spark 1.6.x", Spark1xTest) {

    DQMasterBatch.main(Array("-a", s"$dir/dq-core/src/test/resources/ci.conf", "-c", s"$dir/docs/examples/conf/usgs-depth.conf", "-d", "2019-01-01", "-l"))

    val expectedFileName : String = s"$dir/dq-core/src/test/resources/output/CHECKS-1.6.csv"

    log.info(s"Comparison between $outputFileName and $expectedFileName")
    val resultFile : Array[String] = Source.fromFile(outputFileName).getLines.toArray
    val expectedFile : Array[String] = Source.fromFile(expectedFileName).getLines.toArray

    assert(!resultFile.isEmpty)
    assert(resultFile.sameElements(expectedFile))
  }

  test("must be run with spark 2.4.x", Spark2xTest) {

    DQMasterBatch.main(Array("-a", s"$dir/dq-core/src/test/resources/ci.conf", "-c", s"$dir/docs/examples/conf/usgs-depth.conf", "-d", "2019-01-01", "-l"))

    val expectedFileName : String = s"$dir/dq-core/src/test/resources/output/CHECKS-2.4.csv"

    log.info(s"Comparison between $outputFileName and $expectedFileName")
    val resultFile : Array[String] = Source.fromFile(outputFileName).getLines.toArray
    val expectedFile : Array[String] = Source.fromFile(expectedFileName).getLines.toArray

    assert(!resultFile.isEmpty)
    assert(resultFile.sameElements(expectedFile))
  }
}