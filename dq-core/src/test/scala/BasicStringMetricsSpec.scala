import it.agilelab.bigdata.DataQuality.metrics.ColumnMetrics.BasicStringMetrics.RegexValuesMetricCalculator
import it.agilelab.bigdata.DataQuality.metrics.MetricCalculator
import org.scalatest._

import scala.util.Random

class BasicStringMetricsSpec extends WordSpec with Matchers {

  "RegexValuesMetricCalculator" must {
    val rand = Random
    rand.setSeed(2018)

    "throw exception on empty map" in {
      val params: Map[String, Any] = Map.empty
      a[NoSuchElementException] should be thrownBy {
        new RegexValuesMetricCalculator(params)
      }
    }

    "contain REGEX_VALUES in result" in {
      val params: Map[String, Any] = Map("regex" -> "*")
      val metric = new RegexValuesMetricCalculator(params)
      assert(metric.result().keySet == Set("REGEX_VALUES"))
    }

    "returns correct score" in {
      val params: Map[String, Any] = Map("regex" -> "^[a-z0-9_-]{1,15}$")
      val times: Int = 10
      val values = Seq.fill(times)(rand.nextInt(100).toString)
      val updated = values.foldLeft[MetricCalculator](
        new RegexValuesMetricCalculator(params))((met, v) =>
        met.increment(Seq(v)))
      assert(updated.result()("REGEX_VALUES")._1 == times)
    }
  }

  "CovarianceMetricCalculator" must {
    "throw exception on single column call" in {
      val params: Map[String, Any] = Map.empty
      a[NoSuchElementException] should be thrownBy {
        new RegexValuesMetricCalculator(params).increment(Seq("test"))
      }
    }
  }

}
