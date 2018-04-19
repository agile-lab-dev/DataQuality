package backend

import org.scalatestplus.play._

import scala.collection.immutable.Queue

/**
  * Created by Egor Makhov on 13/09/2017.
  */

class DAGProcessorSpec extends PlaySpec {

  import models.ModelUtils._
  import play.api.Logger

  "Graph processor" must {

    val tg1: Map[String, Seq[String]] = Map(
      "1" -> Seq("3"),
      "2" -> Seq("1"),
      "3" -> Seq("4", "5"),
      "4" -> Seq("ext1"),
      "5" -> Seq("ext3"),
      "6" -> Seq("7"),
      "7" -> Seq("3"),
      "8" -> Seq("ext2")
    )

    val tg2: Map[String, Seq[String]] = Map(
      "4" -> Seq("2","3"),
      "1" -> Seq("e1"),
      "2" -> Seq("1"),
      "3" -> Seq("1")
    )
    val tg3: Map[String, Seq[String]] = Map(
      "1" -> Seq("2"),
      "2" -> Seq("3"),
      "3" -> Seq("4"),
      "4" -> Seq("e1")
    )

    val atg1: Map[String, Seq[String]] = Map(
      "2" -> Seq("1"),
      "1" -> Seq("2")
    )

    val atg2: Map[String, Seq[String]] = Map(
      "1" -> Seq("3", "6"),
      "2" -> Seq("1"),
      "3" -> Seq("4", "5","1"),
      "4" -> Seq("ext1"),
      "5" -> Seq("ext3"),
      "6" -> Seq("7"),
      "7" -> Seq("3"),
      "8" -> Seq("ext2")
    )

    val atg3: Map[String, Seq[String]] = Map(
      "1" -> Seq("2"),
      "2" -> Seq("3"),
      "3" -> Seq("4"),
      "4" -> Seq("1")
    )

    "tg1: return same size seq" in {
      val res = findOptimalOrder(tg1)
      Logger.info(res.mkString(" > "))
      assert(res.size == tg1.keys.size)
    }
    "tg2: return same size seq" in {
      val res = findOptimalOrder(tg2)
      Logger.info(res.mkString(" > "))
      assert(res.size == tg2.keys.size)
    }
    "tg3: return correct order" in {
      val res: Queue[String] = findOptimalOrder(tg3)
      Logger.info(res.mkString(" > "))
      assert(res == Queue("4","3","2","1"))
    }
    "atg1: should throw error" in {
      assertThrows[IllegalArgumentException](findOptimalOrder(atg1))
    }
    "atg2: should throw error" in {
      assertThrows[IllegalArgumentException](findOptimalOrder(atg2))
    }
    "atg3: should throw error" in {
      assertThrows[IllegalArgumentException](findOptimalOrder(atg3))
    }

  }
}