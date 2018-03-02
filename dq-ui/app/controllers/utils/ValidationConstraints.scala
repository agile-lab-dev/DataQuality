package controllers.utils

import controllers.ControllerUtils.errorParameterType
import models.ModelUtils.parseSeparatedString
import models.checks.Check.CheckType
import models.checks.{Check, CheckParameter, SnapshotCheck, TrendCheck}
import models.meta.{CheckMeta, MetaParamType, MetricMeta, MetricParamMeta}
import models.metrics.Metric.MetricType
import models.metrics.{ColumnMetric, FileMetric, Metric, MetricParameter}
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Query
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}

import scala.util.Try

object ValidationConstraints {

    def nonEmptySeq[T]: Constraint[Seq[T]] = Constraint[Seq[T]]("constraint.required") { o =>
    if (o.nonEmpty) Valid else Invalid(ValidationError("error.required"))
  }

  def validateColumnMetric: Constraint[(Metric, ColumnMetric, Seq[MetricParameter])] =
    Constraint[(Metric, ColumnMetric, Seq[MetricParameter])]("constraint.column_metric") { case (met, colMet, params) =>
      inTransaction{
        val metaNames: Seq[String] = MetricMeta.getShortList(Option(MetricType.column)).toList

        val errors = if (metaNames.contains(met.name)) {
          val metricMeta: MetricMeta = MetricMeta.getById(met.name).single
          val colError = if (metricMeta.isMulticolumn && parseSeparatedString(Some(colMet.columns)).size < 2)
            Seq(ValidationError("Two columns should be specified. It's a multicolumn metric"))
          else Seq()

          val requiredParams: Seq[MetricParamMeta] = metricMeta.parameters.toList

          val paramName: Seq[String] = requiredParams.map(_.name)
          val formParams = params.map(_.name)
          val extraParams = formParams.diff(paramName).map(p => ValidationError(s"Parameter '$p' is not needed!"))
          val missingParams = paramName.diff(formParams).map(p => ValidationError(s"Parameter '$p' is missing!"))

          colError ++ extraParams ++ missingParams match {
            case err if err.isEmpty =>
              params.zip(requiredParams).foldLeft(Seq.empty[ValidationError]) {
                case (errs, (param, meta)) =>
                  val validator = MetaParamType.withName(meta.paramType)
                  if (validator.validationFunc.apply(param.value)) errs
                  else errs ++ Seq(ValidationError(
                    errorParameterType(param.name, param.value, validator.toString)))
              }
            case err => err
          }
        } else Seq(ValidationError("Metric name is invalid. Please, check the documentation!"))

        if (errors.isEmpty) Valid else Invalid(errors)
      }
  }

  def validateFileMetric: Constraint[(Metric, FileMetric)] =
    Constraint[(Metric, FileMetric)]("constraint.file_metric") { case (met, _) =>
      inTransaction{
        val metaNames: Seq[String] = MetricMeta.getShortList(Option(MetricType.file)).toList

        if (!metaNames.contains(met.name)) Invalid("Metric name is invalid. Please, check the documentation!")
        else Valid
      }
    }

  def validateSnapshotCheck: Constraint[(Check, SnapshotCheck, Seq[CheckParameter])] =
    Constraint[(Check, SnapshotCheck, Seq[CheckParameter])]("constraint.snapshot_check") { case (check, _, params) =>
      inTransaction{
        val metaNames: Seq[String] = CheckMeta.getShortList(Option(CheckType.snapshot)).toList

        val errors = if (metaNames.contains(check.subtype)) {
          val formParams: Map[String, String] = params.map(p =>p.name -> p.value).toMap
          if (params.size != formParams.size) Seq(ValidationError("Some of the parameters are repeated!"))
          else {
            val checkMeta: Query[CheckMeta] = CheckMeta.getById(check.subtype)
            val requiredModeParams: Iterable[Map[String, String]] = checkMeta.map(meta => meta.parameters.map(p => p.name ->p.paramType).toMap)

            val paramErrors: Iterable[Seq[ValidationError]] = requiredModeParams.map { modeParams =>
              if (modeParams.keySet != formParams.keySet) None
              else {
                Try(modeParams.foldLeft(Seq.empty[ValidationError]){
                  case (errs,(key,t)) =>
                    val tipo: _root_.models.meta.MetaParamType.Value = MetaParamType.withName(t)
                    val value = formParams(key)
                    if(tipo.validationFunc(value)) errs
                    else errs ++ Seq(ValidationError(errorParameterType(key, value, tipo.toString)))
                }).toOption
              }
            }.filter(_.isDefined).map(_.get)

            if (paramErrors.isEmpty) Seq(ValidationError("Cannot fit input to any parameter set. Check the documentation!"))
            else paramErrors.head
          }

        } else Seq(ValidationError("Check subtype is invalid. Please, check the documentation!"))

        if (errors.isEmpty) Valid else Invalid(errors)
      }
    }

  def validateTrendCheck: Constraint[(Check, TrendCheck, Seq[CheckParameter])] =
    Constraint[(Check, TrendCheck, Seq[CheckParameter])]("constraint.trend_check") { case (check, trendCheck, params) =>
      inTransaction{
        val metaNames: Seq[String] = CheckMeta.getShortList(Option(CheckType.trend)).toList

        val errors = check.subtype match {
          case "TOP_N_RANK_CHECK" if !Metric.getTopNList.contains(trendCheck.metric) =>
            Seq(ValidationError("Provided metric is not 'TOP_N'"))
          case currType if metaNames.contains(check.subtype) =>
            val formParams: Map[String, String] = params.map(p =>p.name -> p.value).toMap
            if (params.size != formParams.size) Seq(ValidationError("Some of the parameters are repeated!"))
            else {
              val checkMeta: Query[CheckMeta] = CheckMeta.getById(currType)
              val requiredModeParams: Iterable[Map[String, String]] = checkMeta.map(meta => meta.parameters.map(p => p.name ->p.paramType).toMap)
              val paramErrors: Iterable[Seq[ValidationError]] = requiredModeParams.map { modeParams =>
                if (modeParams.keySet != formParams.keySet) None
                else {
                  val opt = Try(modeParams.foldLeft(Seq.empty[ValidationError]){
                    case (errs,(key,t)) =>
                      val tipo: _root_.models.meta.MetaParamType.Value = MetaParamType.withName(t)
                      val value = formParams(key)
                      if(tipo.validationFunc(value)) errs
                      else errs ++ Seq(ValidationError(errorParameterType(key, value, tipo.toString)))
                  }).toOption
                  opt
                }
              }.filter(_.isDefined).map(_.get)

              if (paramErrors.isEmpty) Seq(ValidationError("Cannot fit input to any parameter set. Check the documentation!"))
              else paramErrors.head
            }
          case _ => Seq(ValidationError("Check subtype is invalid. Please, check the documentation!"))
        }
        if (errors.isEmpty) Valid else Invalid(errors)
      }
    }

  def validateId(id: String, dbCall: Query[String], baseID: Option[String]): Boolean = {
    val list: Seq[String] = dbCall.toList
    !list.contains(id) || Option(id) == baseID
  }

  def validateRefList(ref: String, dbCall: Query[String]): Boolean = {
    val list: Seq[String] = dbCall.toList
    list.contains(ref)
  }

}
