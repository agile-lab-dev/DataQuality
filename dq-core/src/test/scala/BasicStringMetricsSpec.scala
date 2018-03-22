import it.agilelab.bigdata.DataQuality.metrics.ColumnMetrics.BasicStringMetrics.RegexValuesMetricCalculator

import org.scalatest._

class BasicStringMetricsSpec extends WordSpec with Matchers {

  "RegexValuesMetricCalculator" must {

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



  }

//  "A Stack" should "pop values in last-in-first-out order" in {
//    val stack = new Stack[Int]
//    stack.push(1)
//    stack.push(2)
//    stack.pop() should be (2)
//    stack.pop() should be (1)
//  }

//  it should "throw NoSuchElementException if an empty stack is popped" in {
//
//  }
}
