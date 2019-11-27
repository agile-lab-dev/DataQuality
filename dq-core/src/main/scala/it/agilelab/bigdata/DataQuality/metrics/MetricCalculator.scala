package it.agilelab.bigdata.DataQuality.metrics

import it.agilelab.bigdata.DataQuality.metrics
import it.agilelab.bigdata.DataQuality.metrics.CalculatorStatus.CalculatorStatus
import it.agilelab.bigdata.DataQuality.utils.DQSettings

import scala.math.pow
import scala.util.matching.Regex
import scala.util.parsing.combinator.JavaTokenParsers

/**
  * Created by Egor Makhov on 27/04/17.
  */
object CalculatorStatus extends Enumeration {
  type CalculatorStatus = Value
  val OK, FAILED = Value
}

trait StatusableCalculator extends MetricCalculator {
  protected val status: CalculatorStatus
  protected val failCount: Int
  protected def copyWithState(failed: CalculatorStatus): MetricCalculator with StatusableCalculator

  def getFailCounter: Int = failCount

  override def getStatus: metrics.CalculatorStatus.Value = status
}

/**
  * Basic metric calculator
  */
trait MetricCalculator {

  /**
    * Merges two metric calculators together
    *
    * @param m2 second metric calculator
    * @return merged metric calculator
    */
  def merge(m2: MetricCalculator): MetricCalculator

  /**
    * Updates metric calculator with new values
    *
    * @param values values to process
    * @return updated calculator
    */
  def increment(values: Seq[Any]): MetricCalculator

  /**
    * Present results of caclulator in the current state
    *
    * @return Map of (result_name -> result)
    */
  def result(): Map[String, (Double, Option[String])]

  def getStatus: CalculatorStatus = CalculatorStatus.OK

}

/**
  * Takes all metric results and then calculating new ones with formulas
  * @param primitiveMetrics metric results to operate with
  */
class ComposedMetricCalculator(primitiveMetrics: Iterable[MetricResult]) extends ExprParsers2 {

  private lazy val metricsResultMap: Map[String, String] = getMetricResultMap

  /**
    * Processes composed metric
    * @param ex composed metric to operate with
    * @param settings dataquality configuration
    * @return composed metric result
    */
  def run(ex: ComposedMetric)(implicit settings: DQSettings) = {
    val formulaWithParameters = ex.formula
    val formulaWithValues     = replaceMetricsInFormula(formulaWithParameters)

    val result = calculateFormula(formulaWithValues)

    ComposedMetricResult(ex.id, settings.refDateString, ex.name, "", formulaWithParameters, result, "")
  }

  /**
    * Parses the formula and calculates the result
    * @param formula input formula
    * @return numeric result
    */
  private def calculateFormula(formula: String) = {
    val parsed = parseAll(expr, replaceMetricsInFormula(formula)).get
    eval(parsed)
  }

  private def replacer: (Regex.Match) => String = (a: Regex.Match) => {
    val id = a.group(1)
    metricsResultMap(id)
  }

  /**
    * Replaces metric ids with their results
    * @param ex string with metric ids
    * @return string with results
    */
  private def replaceMetricsInFormula(ex: String): String = {
    val rgx = "\\$([a-zA-Z0-9_:]+)".r
    rgx.replaceAllIn(ex, replacer)
  }

  /**
    * Trims input metrics
    * @return map of (Metric_id -> Result)
    */
  private def getMetricResultMap: Map[String, String] = {
    primitiveMetrics.map { primitiveMetric =>
      primitiveMetric.metricId -> primitiveMetric.result.toString
    }.toMap
  }
}

/**
  * Recursively creates a tree of operations and executes it
  */
sealed trait ExprParsers2 extends JavaTokenParsers {

  sealed abstract class Tree
  case class Add(t1: Tree, t2: Tree) extends Tree
  case class Sub(t1: Tree, t2: Tree) extends Tree
  case class Mul(t1: Tree, t2: Tree) extends Tree
  case class Div(t1: Tree, t2: Tree) extends Tree
  case class Pow(t1: Tree, t2: Tree) extends Tree
  case class Num(t: Double)          extends Tree

  def eval(t: Tree): Double = t match {
    case Add(t1, t2) => eval(t1) + eval(t2)
    case Sub(t1, t2) => eval(t1) - eval(t2)
    case Mul(t1, t2) => eval(t1) * eval(t2)
    case Div(t1, t2) => eval(t1) / eval(t2)
    case Pow(t1, t2) => pow(eval(t1), eval(t2))
    case Num(t)      => t
  }

  lazy val expr: Parser[Tree] = term ~ rep("[+-]".r ~ term) ^^ {
    case t ~ ts =>
      ts.foldLeft(t) {
        case (t1, "+" ~ t2) => Add(t1, t2)
        case (t1, "-" ~ t2) => Sub(t1, t2)
      }
  }

  lazy val term = factor ~ rep("[*/^]".r ~ factor) ^^ {
    case t ~ ts =>
      ts.foldLeft(t) {
        case (t1, "*" ~ t2) => Mul(t1, t2)
        case (t1, "/" ~ t2) => Div(t1, t2)
        case (t1, "^" ~ t2) => Pow(t1, t2)
      }
  }

  lazy val factor = "(" ~> expr <~ ")" | num

  lazy val num = floatingPointNumber ^^ { t =>
    Num(t.toDouble)
  }

}
