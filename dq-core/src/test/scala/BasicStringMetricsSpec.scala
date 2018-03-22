import it.agilelab.bigdata.DataQuality.metrics.ColumnMetrics.BasicStringMetrics.RegexValuesMetricCalculator
import org.scalatest._

import scala.util.Random

class BasicStringMetricsSpec extends WordSpec with Matchers {

  "RegexValuesMetricCalculator" must {
    val rand = Random
    rand.setSeed(2018)

    "throw exception on empty map" in {
      val params: Map[String, Any] = Map.empty
      a [NoSuchElementException] should be thrownBy {
        new RegexValuesMetricCalculator(params)
      }
    }

    "contain REGEX_VALUES in result" in {
      val params: Map[String, Any] = Map("regex"->"*")
      val metric = new RegexValuesMetricCalculator(params)
      assert(metric.result().keySet == Set("REGEX_VALUES"))
    }

    "returns correct score" in {
      val params: Map[String, Any] = Map("regex"->"^[a-z0-9_-]{1,15}$")
      val metric = new RegexValuesMetricCalculator(params)
      val values: Seq[String] = Seq(rand.nextInt(100).toString)
      val updated = metric.increment(values).increment(values).increment(values).increment(values)
      assert(updated.result()("REGEX_VALUES")._1 == 4)
    }
  }
}
