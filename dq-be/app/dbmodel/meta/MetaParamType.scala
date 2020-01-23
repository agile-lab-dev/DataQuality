package dbmodel.meta

import dbmodel.metrics.Metric
import org.joda.time.format.DateTimeFormat
import org.squeryl.PrimitiveTypeMode._

import scala.util.Try

/**
  * Created by Egor Makhov on 31/10/2017.
  */

object MetaParamType extends Enumeration {
  val double = ParamValidationVal("DOUBLE", validateDouble)
  val integer = ParamValidationVal("INTEGER", validateInt)
  val string = ParamValidationVal("STRING", validateString)
  val metric = ParamValidationVal("METRIC", validateMetric)
  val dateFormat = ParamValidationVal("DATE_FORMAT", validateDateFormat)
  val proportion = ParamValidationVal("PROPORTION", validateProportion)

  protected case class ParamValidationVal(name: String, validationFunc: (String)=>Boolean) extends super.Val() {
    override def toString(): String = this.name
  }
  implicit def convert(value: Value): ParamValidationVal = value.asInstanceOf[ParamValidationVal]

  private def validateDouble(value: String): Boolean = Try(value.toDouble).isSuccess
  private def validateInt(value: String): Boolean = Try(value.toInt).isSuccess
  private def validateString(value: String): Boolean = Try(value.toString).isSuccess
  private def validateMetric(value: String): Boolean = Metric.getIdList().toList.contains(value)
  private def validateDateFormat(value: String): Boolean = Try{DateTimeFormat.forPattern(value)}.isSuccess
  private def validateProportion(value: String): Boolean = {
    Try(value.toDouble).toOption match {
      case Some(v) if v >= 0 && v <= 1 => true
      case _ => false
    }
  }
}


