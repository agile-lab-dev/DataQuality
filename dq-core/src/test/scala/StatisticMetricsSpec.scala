import org.scalatest.{FlatSpec, Matchers, WordSpec}

import scala.util.Random

class StatisticMetricsSpec extends WordSpec with Matchers {

  val rand = new Random()
  rand.setSeed(123)

  val configMap: Map[String,String] = Map(
    "test" -> "test",
    "toast" -> "Toast"
  )

  "Statistic multi-column metric" must {
    "be cool" in {

      println(configMap)
      assert(true)
    }

  }

}
