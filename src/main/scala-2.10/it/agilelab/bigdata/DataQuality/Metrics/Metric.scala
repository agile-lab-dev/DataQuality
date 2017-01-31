package it.agilelab.bigdata.DataQuality.metrics

import it.agilelab.bigdata.DataQuality.exceptions.IllegalParameterException
import org.apache.spark.sql.DataFrame
import org.joda.time.format.DateTimeFormat

import scala.util.{Failure, Success, Try}


/**
  * Created by Gianvito Siciliano on 29/12/16.
  */


trait Metric{
  def id:String
  def name: String
  def description: String
  def paramMap: Map[String, Any]
}

case class ColumnMetric(
                         id:String,
                         name: String,
                         description: String,
                         source: String,
                         column: String,
                         paramMap: Map[String, Any]
                       ) extends Metric



case class FileMetric(
                       id:String,
                       name: String,
                       description: String,
                       source: String,
                       paramMap: Map[String, Any]
                     ) extends Metric

case class ComposedMetric(
                           id:String,
                           name: String,
                           description: String,
                           formula: String,
                           paramMap: Map[String, Any]
                         ) extends Metric



trait MetricCalculator {

  def merge(m2: MetricCalculator): MetricCalculator

  def increment(value: Any): MetricCalculator

  def result(): Double

}


//COLUMN METRICS
case class UniqueValuesMatricCalculator(values: Set[Any]) extends MetricCalculator {

  override def increment(value: Any): MetricCalculator = {
    UniqueValuesMatricCalculator(values + value)
  }

  override def result() = values.size

  override def merge(m2: MetricCalculator): MetricCalculator = UniqueValuesMatricCalculator(this.values ++ m2.asInstanceOf[UniqueValuesMatricCalculator].values)

}

case class NullValuesMatricCalculator(cnt: Int) extends MetricCalculator {

  override def increment(value: Any): MetricCalculator = {
    NullValuesMatricCalculator(cnt + (if(value == null) 1 else 0) )
  }

  override def result() = cnt

  override def merge(m2: MetricCalculator): MetricCalculator = NullValuesMatricCalculator(this.cnt + m2.asInstanceOf[NullValuesMatricCalculator].cnt)

}

case class EmptyStringValuesMatricCalculator(cnt: Int) extends MetricCalculator {

  override def increment(value: Any): MetricCalculator = {
    EmptyStringValuesMatricCalculator(cnt + (if(value.isInstanceOf[String] && value.toString == "") 1 else 0))
  }

  override def result() = cnt

  override def merge(m2: MetricCalculator): MetricCalculator = EmptyStringValuesMatricCalculator(this.cnt + m2.asInstanceOf[EmptyStringValuesMatricCalculator].cnt)

}

case class MinNumericValueMatricCalculator(min: Double) extends MetricCalculator {

  override def increment(value: Any): MetricCalculator = {
    val possibleMin = Try{value.asInstanceOf[Double]}.getOrElse(Double.MaxValue)

    MinNumericValueMatricCalculator(Math.min(possibleMin, min))
  }

  override def result() = min

  override def merge(m2: MetricCalculator): MetricCalculator = MinNumericValueMatricCalculator(Math.min(this.min, m2.asInstanceOf[MinNumericValueMatricCalculator].min))

}

case class MaxNumericValueMatricCalculator(max: Double) extends MetricCalculator {

  override def increment(value: Any): MetricCalculator = {
    val possibleMax = Try{value.asInstanceOf[Double]}.getOrElse(Double.MinValue)

    MinNumericValueMatricCalculator(Math.max(possibleMax, max))
  }

  override def result() = max

  override def merge(m2: MetricCalculator): MetricCalculator = MaxNumericValueMatricCalculator(Math.max(this.max, m2.asInstanceOf[MaxNumericValueMatricCalculator].max))

}

case class SumNumericValueMatricCalculator(sum: Double) extends MetricCalculator {

  override def increment(value: Any): MetricCalculator = {
    SumNumericValueMatricCalculator(sum + Try{value.asInstanceOf[Double]}.getOrElse(0D))
  }

  override def result() = sum

  override def merge(m2: MetricCalculator): MetricCalculator = SumNumericValueMatricCalculator(this.sum + m2.asInstanceOf[SumNumericValueMatricCalculator].sum)

}

case class AvgNumericValueMatricCalculator(sum: Double, cnt: Int) extends MetricCalculator {

  override def increment(value: Any): MetricCalculator = {
    val (vv, bb) = Try{value.asInstanceOf[Double]} match {
      case Success(vv) => (vv, true)
      case Failure(vv) => (0D, false)
    }
    AvgNumericValueMatricCalculator(
      sum + vv,
      cnt + (if (bb) 1 else 0)
    )
  }

  override def result() = sum / cnt.toDouble

  override def merge(m2: MetricCalculator): MetricCalculator = {
    val cm2 = m2.asInstanceOf[AvgNumericValueMatricCalculator]
    AvgNumericValueMatricCalculator(
      this.sum + cm2.sum,
      this.cnt + cm2.cnt
    )
  }


}

case class MinStringValueMatricCalculator(strl: Double) extends MetricCalculator {

  override def increment(value: Any): MetricCalculator = {
    MinNumericValueMatricCalculator(Math.min(value.toString.length, strl))
  }

  override def result() = strl

  override def merge(m2: MetricCalculator): MetricCalculator = MinStringValueMatricCalculator(Math.min(this.strl, m2.asInstanceOf[MinStringValueMatricCalculator].strl))

}

case class MaxStringValueMatricCalculator(strl: Double) extends MetricCalculator {

  override def increment(value: Any): MetricCalculator = {
    MinNumericValueMatricCalculator(Math.max(value.toString.length, strl))
  }

  override def result() = strl

  override def merge(m2: MetricCalculator): MetricCalculator = MaxStringValueMatricCalculator(Math.max(this.strl, m2.asInstanceOf[MaxStringValueMatricCalculator].strl))

}

case class AvgStringValueMatricCalculator(sum: Double, cnt: Int) extends MetricCalculator {

  override def increment(value: Any): MetricCalculator = {
    val (vv, bb) = Try{value.asInstanceOf[String]} match {
      case Success(vv) => (vv.length.toDouble, true)
      case Failure(vv) => (0D, false)
    }
    AvgStringValueMatricCalculator(
      sum + vv,
      cnt + (if (bb) 1 else 0)
    )
  }

  override def result() = sum / cnt.toDouble

  override def merge(m2: MetricCalculator): MetricCalculator = {
    val cm2 = m2.asInstanceOf[AvgStringValueMatricCalculator]
    AvgStringValueMatricCalculator(
      this.sum + cm2.sum,
      this.cnt + cm2.cnt
    )
  }


}

case class DateFormattedValuesMatricCalculator(cnt: Double, formatDate:String) extends MetricCalculator {

  override def increment(value: Any): MetricCalculator = {
    DateFormattedValuesMatricCalculator(cnt + (if(checkDate(value, formatDate)) 1 else 0), formatDate)
  }

  override def result() = cnt

  override def merge(m2: MetricCalculator): MetricCalculator = DateFormattedValuesMatricCalculator(this.cnt + m2.asInstanceOf[DateFormattedValuesMatricCalculator].cnt, formatDate)

  private def checkDate(value:Any, dateFormat:String) = {
    val fmt = DateTimeFormat forPattern formatDate
    Try{fmt parseDateTime value.asInstanceOf[String]}.isSuccess
  }
}

case class StringFormattedValuesMatricCalculator(cnt: Double, length:Int) extends MetricCalculator {

  override def increment(value: Any): MetricCalculator = {
    StringFormattedValuesMatricCalculator(cnt + (if(value.asInstanceOf[String].length<=length) 1 else 0), length)
  }

  override def result() = cnt

  override def merge(m2: MetricCalculator): MetricCalculator = StringFormattedValuesMatricCalculator(this.cnt + m2.asInstanceOf[StringFormattedValuesMatricCalculator].cnt, length)

}

case class NumberFormattedValuesMatricCalculator(cnt: Double, precOpt:Option[Any], scaleOpt:Option[Any]) extends MetricCalculator {

  val prec = precOpt match{
    case Some(p) => Option(p.toString)
    case None    => None
  }

  val scale = scaleOpt match{
    case Some(s) => Option(s.toString)
    case None    => None
  }

  override def increment(value: Any): MetricCalculator = {
    val (typeErr, precErr, scaleErr) = checkNumber(value, prec, scale)
    NumberFormattedValuesMatricCalculator(cnt + (if(typeErr!=0 && precErr !=0 && scaleErr!=0) 1 else 0), prec, scale)
  }

  override def result() = cnt

  override def merge(m2: MetricCalculator): MetricCalculator = NumberFormattedValuesMatricCalculator(this.cnt+ m2.asInstanceOf[NumberFormattedValuesMatricCalculator].cnt, prec, scale)

  //return (typeError, scaleErr, precErr) -> [0,1]
  private def checkNumber(value:Any, optPrec:Option[String], optScale:Option[String]): (Int, Int, Int) = {
    Try{value.asInstanceOf[Double]} match {
      case Failure(e) => (0,0,0)
      case Success(e) => {
        val scaleError = optScale match {
          case None        => 0
          case Some(scale) => {
            val split = value.asInstanceOf[String].split("\\.")
            if (split.length == 1) 0
            else if (split(0).length <= scale.toInt) 0
            else 1
          }
        }
        val precError = optPrec match {
          case None        => 0
          case Some(prec) => {
            val strVal = value.asInstanceOf[String]
            val ll = if(strVal.contains(".")) strVal.length-1 else strVal.length
            if (ll <= prec.toInt) 0 else 1
          }
        }
        (1, scaleError, precError)
      }
    }
  }
}

case class NumberCastValuesMatricCalculator(cnt: Double) extends MetricCalculator {

  override def increment(value: Any): MetricCalculator = {
    NumberCastValuesMatricCalculator(cnt + (if (Try{value.asInstanceOf[Double]}.isSuccess) 1 else 0 ))
  }

  override def result() = cnt

  override def merge(m2: MetricCalculator): MetricCalculator = NumberCastValuesMatricCalculator(this.cnt + m2.asInstanceOf[NumberCastValuesMatricCalculator].cnt)

}

case class NumberInDomainValuesMatricCalculator(cnt: Double, values: Set[Double]) extends MetricCalculator {

  override def increment(value: Any): MetricCalculator = {
    NumberInDomainValuesMatricCalculator(cnt + (if (Try{value.asInstanceOf[Double]}.isSuccess && values.contains(value.toString.toDouble)) 1 else 0 ), values)
  }

  override def result() = cnt

  override def merge(m2: MetricCalculator): MetricCalculator = NumberInDomainValuesMatricCalculator(this.cnt + m2.asInstanceOf[NumberInDomainValuesMatricCalculator].cnt, values)

}

case class NumberOutDomainValuesMatricCalculator(cnt: Double, values: Set[Double]) extends MetricCalculator {

  override def increment(value: Any): MetricCalculator = {
    NumberOutDomainValuesMatricCalculator(cnt + (if (Try{value.asInstanceOf[Double]}.isSuccess && !values.contains(value.toString.toDouble)) 1 else 0 ), values)
  }

  override def result() = cnt

  override def merge(m2: MetricCalculator): MetricCalculator = NumberOutDomainValuesMatricCalculator(this.cnt + m2.asInstanceOf[NumberOutDomainValuesMatricCalculator].cnt, values)

}

case class StringInDomainValuesMatricCalculator(cnt: Double, values: Set[String]) extends MetricCalculator {

  override def increment(value: Any): MetricCalculator = {
    StringInDomainValuesMatricCalculator(cnt + (if (Try{value.asInstanceOf[String]}.isSuccess && values.contains(value.toString)) 1 else 0 ), values)
  }

  override def result() = cnt

  override def merge(m2: MetricCalculator): MetricCalculator = StringInDomainValuesMatricCalculator(this.cnt + m2.asInstanceOf[StringInDomainValuesMatricCalculator].cnt, values)

}

case class StringOutDomainValuesMatricCalculator(cnt: Double, values: Set[String]) extends MetricCalculator {

  override def increment(value: Any): MetricCalculator = {
    StringOutDomainValuesMatricCalculator(cnt + (if (Try{value.asInstanceOf[String]}.isSuccess && !values.contains(value.toString)) 1 else 0 ), values)
  }

  override def result() = cnt

  override def merge(m2: MetricCalculator): MetricCalculator = StringOutDomainValuesMatricCalculator(this.cnt + m2.asInstanceOf[StringOutDomainValuesMatricCalculator].cnt, values)

}

case class NumberValuesMatricCalculator(cnt: Int, lvalue:Double) extends MetricCalculator {

  override def increment(value: Any): MetricCalculator = {
    NumberValuesMatricCalculator(cnt + (if(value == lvalue) 1 else 0), lvalue )
  }

  override def result() = cnt

  override def merge(m2: MetricCalculator): MetricCalculator = NumberValuesMatricCalculator(this.cnt + m2.asInstanceOf[NumberValuesMatricCalculator].cnt, lvalue)

}

case class StringValuesMatricCalculator(cnt: Int, lvalue:String) extends MetricCalculator {

  override def increment(value: Any): MetricCalculator = {
    StringValuesMatricCalculator(cnt + (if(value == lvalue) 1 else 0), lvalue )
  }

  override def result() = cnt

  override def merge(m2: MetricCalculator): MetricCalculator = StringValuesMatricCalculator(this.cnt + m2.asInstanceOf[StringValuesMatricCalculator].cnt, lvalue)

}


//FILE METRIC
case class RowCountMatricCalculator(cnt: Int) extends MetricCalculator {

  override def increment(value: Any): MetricCalculator = RowCountMatricCalculator(cnt+1)

  override def result() = cnt

  override def merge(m2: MetricCalculator): MetricCalculator = RowCountMatricCalculator(this.cnt+m2.asInstanceOf[RowCountMatricCalculator].cnt)
}





object SourceProcessor {

  type CheckId = String

  type MetricId = String

  type FileId = String

  type ColumnId = String


  def processFileMetrics(df: DataFrame, metrics: Seq[FileMetric]) : Map[FileMetric, MetricCalculator] = {

    val accumulators: Map[FileMetric, MetricCalculator] = metrics.map{ mm => {
      val calc = mm.name match {
        case "ROW_COUNT"        => RowCountMatricCalculator(0)  //return rows count
        case x => throw new IllegalParameterException(x)
      }
      mm -> calc
    }}.toMap

    val results: Map[FileMetric, MetricCalculator] = df.rdd.aggregate(accumulators)(
      //SeqOp
      (acc, value) => acc.map(a => (a._1, a._2.increment(value))),

      //CombOp
      (acc1, acc2) => acc1.map( a => (a._1, a._2.merge(acc2(a._1)))
      )
    )
    results
  }

  def processColumnMetrics(df: DataFrame, metrics: Seq[ColumnMetric]): Map[ColumnId, Map[ColumnMetric, MetricCalculator]] = {

    val metricsByColumn: Map[ColumnId, Seq[ColumnMetric]] = metrics.groupBy(_.column)

    val columnsIndexes: Map[String, Int] = df.schema.fieldNames.map(s => s -> df.schema.fieldIndex(s)).toMap

    //set calculator for each metric
    val calculators: Map[ColumnId, Map[ColumnMetric, MetricCalculator]] = metricsByColumn.map{ case (colId, metrics) => {

      colId -> metrics.map( mm => {
        val calc = mm.name match {
          case "DISTINCT_VALUES"    => UniqueValuesMatricCalculator(Set.empty[Any])
          case "NULL_VALUES"        => NullValuesMatricCalculator(0)
          case "EMPTY_VALUES"       => EmptyStringValuesMatricCalculator(0)
          case "MIN_NUMBER"         => MinNumericValueMatricCalculator(Double.MaxValue)
          case "MAX_NUMBER"         => MaxNumericValueMatricCalculator(Double.MinValue)
          case "SUM_NUMBER"         => SumNumericValueMatricCalculator(0)
          case "AVG_NUMBER"         => AvgNumericValueMatricCalculator(0, 0)
          case "MIN_STRING"         => MinStringValueMatricCalculator(0)
          case "MAX_STRING"         => MaxStringValueMatricCalculator(0)
          case "AVG_STRING"         => AvgStringValueMatricCalculator(0, 0)
          case "FORMATTED_DATE"     => DateFormattedValuesMatricCalculator(0, mm.paramMap("dateFormat").toString)
          case "FORMATTED_NUMBER"   => NumberFormattedValuesMatricCalculator(0, mm.paramMap.get("precision"), mm.paramMap.get("scale"))
          case "FORMATTED_STRING"   => StringFormattedValuesMatricCalculator(0, mm.paramMap("length").toString.toInt)
          case "CASTED_NUMBER"      => NumberCastValuesMatricCalculator(0)
          case "NUMBER_IN_DOMAIN"   => NumberInDomainValuesMatricCalculator(0, mm.paramMap("domainSet").asInstanceOf[Set[Double]])
          case "NUMBER_OUT_DOMAIN"  => NumberOutDomainValuesMatricCalculator(0, mm.paramMap("domainSet").asInstanceOf[Set[Double]])
          case "STRING_IN_DOMAIN"   => StringInDomainValuesMatricCalculator(0, mm.paramMap("domainSet").asInstanceOf[Set[String]])
          case "STRING_OUT_DOMAIN"  => StringOutDomainValuesMatricCalculator(0, mm.paramMap("domainSet").asInstanceOf[Set[String]])
          case "STRING_VALUES"        => StringValuesMatricCalculator(0, mm.paramMap("compareValue").toString)
          case "NUMBER_VALUES"        => NumberValuesMatricCalculator(0, mm.paramMap("compareValue").toString.toDouble)

        }
        mm -> calc
      }).toMap
    }}

    //calculate metrics
    val results: Map[ColumnId, Map[ColumnMetric, MetricCalculator]] = df.rdd.aggregate(calculators)((res, row) => {
      val updatedRes: Map[ColumnId, Map[ColumnMetric, MetricCalculator]] = metricsByColumn.map(m => {
        val columnValue = row.get(columnsIndexes(m._1))

        val incrementedMetrics = m._2.map(mm => (mm, res(m._1)(mm).increment(columnValue))).toMap
        (m._1, incrementedMetrics)
      })
      updatedRes

    }, (r,l) => {
      val merged: Map[ColumnId, Map[ColumnMetric, MetricCalculator]] = l.map(c =>
        (c._1, c._2.map(m => {
          val toMerge = r(c._1)(m._1)
          (m._1, m._2.merge(toMerge))
        }))
      )
      merged
    })

    results
  }

}
