package models

import play.api.Logger

import scala.collection.immutable.Queue

/**
  * Created by Egor Makhov on 18/10/2017.
  */
object ModelUtils {

  private lazy val separator: String = ","

  def toSeparatedString(seq: Seq[String]): Option[String] = {
    if (seq.isEmpty) None else Some(seq.mkString(separator))
  }

  def parseSeparatedString(str: Option[String]): Seq[String] = {
    str match {
      case Some(kf) => kf.split(",").toSeq
      case None => Seq.empty
    }
  }

  def findOptimalOrder(graph: Map[String, Seq[String]]): Queue[String] = {

    def loop(current: (String, Seq[String]))(gr: Map[String, Seq[String]], o: Queue[String], v: Set[String]): Queue[String] = {
      Logger.debug(s"Processing tuple: $current")
      Logger.debug(s"Current state: v{${v.mkString(",")}} : o{${o.mkString(",")}}")
      if (v.contains(current._1)) throw new IllegalArgumentException("Graph isn't acyclic.")
      else if (o.contains(current._1)) o
      else {
        val resolved: Queue[String] = current._2.foldLeft(o) {
          case (agg, curr) =>
            curr match {
              case x if x == current._1 => throw new IllegalArgumentException("Graph isn't acyclic.")
              case x if !o.contains(x) =>
                gr.get(x) match {
                  case Some(tail) =>
                    Logger.debug(s"Next: ${current._1} -> $x")
                    loop((x, tail))(gr, agg, v + current._1)
                  case None => Queue.empty[String]
                }
              case _ => Queue.empty[String]
            }
        }
        val order: Queue[String] = if (resolved.nonEmpty) resolved :+ current._1 else o :+ current._1
        Logger.debug(s"Put ${current._1} to o: {${order.mkString(",")}}")
        order
      }
    }

    graph.toSeq.foldLeft(Queue.empty[String]) {
      case (ord: Queue[String], curr: (String, Seq[String])) =>
        val r = loop(curr)(graph, ord, Set.empty)
        Logger.debug(s"${curr._1} :: ${r.mkString(", ")}")
        r
    }
  }

}
