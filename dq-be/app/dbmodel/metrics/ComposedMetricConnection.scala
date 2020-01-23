package dbmodel.metrics

import dbmodel.AppDB
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.annotations.Column

import scala.collection.immutable.Seq
import scala.util.matching.Regex
import scala.util.parsing.combinator.JavaTokenParsers
/**
  * Created by Egor Makhov on 21/09/2017.
  */
object ComposedMetricValidator extends ExprParsers2{

  def getConnections(id: String, formula:String): Seq[ComposedMetricConnection] = {
    val metList = getMetricsFromFormula(formula)
    metList.map(met => ComposedMetricConnection(id, met)).toList
  }

  def validateFormula(formula: String, currId: Option[String] = None): Boolean = {
    val formulaMetrics = getMetricsFromFormula(formula)
    val metList = currId match {
      case Some(curr) => Metric.getIdList().toSet - curr
      case None => Metric.getIdList().toSet
    }

    (formulaMetrics.intersect(metList) == formulaMetrics) && getTree(formula).successful
  }

  def deleteFormulaById(id: String): Int = {
    AppDB.composedMetricConnectionsTable.deleteWhere(con => con.composedMetric === id
      or con.formulaMetric === id)
  }

  private def getTree(ex: String) = {
      parseAll(expr, ex)
  }

  private def getMetricsFromFormula(ex: String): Set[String] = {
    // Cutting dollar sign
    rgx.findAllIn(ex).map(met => met.substring(1)).toSet
  }
}

case class ComposedMetricConnection(
                                     @Column("composed_metric")
                                     composedMetric: String,
                                     @Column("formula_metric")
                                     formulaMetric: String
                                   ) {
  def insert(): ComposedMetricConnection = AppDB.composedMetricConnectionsTable.insert(this)
}

// Should be the same as in the DQ core
sealed trait ExprParsers2 extends JavaTokenParsers {

  protected val rgx: Regex = "\\$([a-zA-Z0-9_:]+)".r

  sealed abstract class Tree
  case class Add(t1: Tree, t2: Tree) extends Tree
  case class Sub(t1: Tree, t2: Tree) extends Tree
  case class Mul(t1: Tree, t2: Tree) extends Tree
  case class Div(t1: Tree, t2: Tree) extends Tree
  case class Pow(t1: Tree, t2: Tree) extends Tree
  case class Num(t: Double) extends Tree
  case class Met(t: String) extends Tree


  lazy val expr: Parser[Tree] = term ~ rep("[+-]".r ~ term) ^^ {
    case t ~ ts => ts.foldLeft(t) {
      case (t1, "+" ~ t2) => Add(t1, t2)
      case (t1, "-" ~ t2) => Sub(t1, t2)
    }
  }

  lazy val term = factor ~ rep("[*/^]".r ~ factor) ^^ {
    case t ~ ts => ts.foldLeft(t) {
      case (t1, "*" ~ t2) => Mul(t1, t2)
      case (t1, "/" ~ t2) => Div(t1, t2)
      case (t1, "^" ~ t2) => Pow(t1, t2)
    }
  }

  lazy val factor = "(" ~> expr <~ ")" | num | met
  lazy val num = floatingPointNumber ^^ { t => Num(t.toDouble) }
  lazy val met = rgx ^^ { t => Met(t.toString)}

}
