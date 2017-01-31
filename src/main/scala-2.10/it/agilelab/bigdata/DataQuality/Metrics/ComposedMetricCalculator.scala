package it.agilelab.bigdata.DataQuality.metrics

import scala.util.matching.Regex
import scala.util.parsing.combinator.JavaTokenParsers

class ComposedMetricCalculator(primitiveMetrics: Iterable[MetricResult]) extends ExprParsers2{

  private lazy val metricsResultMap: Map[String, String] = getMetricResultMap

  def run(ex: ComposedMetric) = {
    val formulaWithParameters = ex.formula
    val formulaWithValues = replaceMetricsInFormula(formulaWithParameters)

    val result = calculateFormula(formulaWithValues )

    ComposedMetricResult(ex.id,ex.name, "TODO_sourceId_compMetric", formulaWithParameters, result)
  }

  private def calculateFormula(formula: String) = {
    val parsed = parseAll(expr, replaceMetricsInFormula(formula)).get
    eval(parsed)
  }

  private def replacer: (Regex.Match) => String = (a: Regex.Match) => {
    val id = a.group(1)
    metricsResultMap.get(id).get
  }

  private def replaceMetricsInFormula(ex: String): String = {
    val rgx = "\\$([a-zA-Z0-9]+)".r
    rgx.replaceAllIn(ex, replacer)
  }

  private def getMetricResultMap: Map[String, String] = {
    primitiveMetrics.map {
      primitiveMetric => primitiveMetric.id -> primitiveMetric.result.toString
    }.toMap
  }
}

sealed trait ExprParsers2 extends JavaTokenParsers {

  sealed abstract class Tree
  case class Add(t1: Tree, t2: Tree) extends Tree
  case class Sub(t1: Tree, t2: Tree) extends Tree
  case class Mul(t1: Tree, t2: Tree) extends Tree
  case class Div(t1: Tree, t2: Tree) extends Tree
  case class Num(t: Double) extends Tree

  def eval(t: Tree): Double = t match {
    case Add(t1, t2) => eval(t1)+eval(t2)
    case Sub(t1, t2) => eval(t1)-eval(t2)
    case Mul(t1, t2) => eval(t1)*eval(t2)
    case Div(t1, t2) => eval(t1)/eval(t2)
    case Num(t) => t
  }

  lazy val expr: Parser[Tree] = term ~ rep("[+-]".r ~ term) ^^ {
    case t ~ ts => ts.foldLeft(t) {
      case (t1, "+" ~ t2) => Add(t1, t2)
      case (t1, "-" ~ t2) => Sub(t1, t2)
    }
  }

  lazy val term = factor ~ rep("[*/]".r ~ factor) ^^ {
    case t ~ ts => ts.foldLeft(t) {
      case (t1, "*" ~ t2) => Mul(t1, t2)
      case (t1, "/" ~ t2) => Div(t1, t2)
    }
  }

  lazy val factor = "(" ~> expr <~ ")" | num

  lazy val num = floatingPointNumber ^^ { t => Num(t.toDouble) }


}