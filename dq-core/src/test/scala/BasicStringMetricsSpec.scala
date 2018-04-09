import it.agilelab.bigdata.DataQuality.metrics.ColumnMetrics.BasicStringMetrics.RegexValuesMetricCalculator
import it.agilelab.bigdata.DataQuality.metrics.ColumnMetrics.MultiColumnMetrics.CovarianceMetricCalculator
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

    "returns correct score" in {
      val params: Map[String, Any] = Map("regex" -> "^[a-z0-9_-]{1,15}$")
      val times: Int = 10
      val values = Seq.fill(times)(rand.nextInt(100).toString)
      val updated = values.foldLeft[MetricCalculator](
        new RegexValuesMetricCalculator(params))((met, v) =>
        met.increment(Seq(v)))
      assert(updated.result()("REGEX_VALUES:^[a-z0-9_-]{1,15}$")._1 == times)
    }
  }

  "CovarianceMetricCalculator" must {

    def mean(xs: Seq[Double]): Option[Double] =
      if (xs.isEmpty) None
      else Some(xs.sum / xs.length)

    def variance(xs: Seq[Double]): Option[Double] = {
      mean(xs).flatMap(m => mean(xs.map(x => Math.pow(x-m, 2))))
    }

    val params: Map[String, Any] = Map.empty
    val seq1: Seq[Double] = Seq(1, 2, 3, 4, 5, 6)
    val seq2: Seq[Double] = Seq(20, 31, 48, 54, 32, 1)

    "throw exception on single column call" in {
      a[NoSuchElementException] should be thrownBy {
        new CovarianceMetricCalculator(params).increment(Seq("test"))
      }
    }

    "basic covariance test" in {
      val mc = seq1.zip(seq2).foldLeft[MetricCalculator](new CovarianceMetricCalculator(params))((met, vals) =>
        met.increment(Seq(vals._1, vals._2))
      ).asInstanceOf[CovarianceMetricCalculator]

      assert(mc.lMean == seq1.sum / seq1.length)
      assert(mc.rMean == seq2.sum / seq2.length)
      assert(mc.n == seq1.length)
    }

    "self covariance test" in {
      val mc = seq2.zip(seq2).foldLeft[MetricCalculator](new CovarianceMetricCalculator(params))((met, vals) =>
        met.increment(Seq(vals._1, vals._2))
      ).asInstanceOf[CovarianceMetricCalculator]

      assert(mc.lMean == mc.rMean)
      assert(mc.lMean == mean(seq2).get)
      assert(mc.n == seq2.length)
      assert(mc.result()("COVARIANCE")._1 == variance(seq2).get)
    }

    "zero covariance test" in {
      val seq: Seq[Double] = Seq.fill(100)(1.0)
      val mc = seq.zip(seq).foldLeft[MetricCalculator](new CovarianceMetricCalculator(params))((met, vals) =>
        met.increment(Seq(vals._1, vals._2))
      ).asInstanceOf[CovarianceMetricCalculator]

      assert(mc.lMean == mc.rMean)
      assert(mc.lMean == mean(seq).get)
      assert(mc.n == seq.length)
      assert(mc.result()("COVARIANCE")._1 == variance(seq).get)
    }

  }
}
