package it.agilelab.bigdata.DataQuality.metrics

import it.agilelab.bigdata.DataQuality.exceptions.IllegalParameterException
import it.agilelab.bigdata.DataQuality.metrics.FileMetrics.FileMetrics.RowCountMetricCalculator
import it.agilelab.bigdata.DataQuality.utils
import it.agilelab.bigdata.DataQuality.utils.{DQSettings, Logging}
import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.{Accumulable, SparkContext}

import scala.collection.mutable

object MetricProcessor extends Logging {

  // Some custom types to increase readability of the code
  type ParamMap = Map[String, Any]

  /**
    * Processes all specified metrics for a specific dataframe
    *
    * @param df          dataframe to calculate metrics
    * @param colMetrics  list of column metrics
    * @param fileMetrics list of file metrics
    * @return two maps (for column and file metrics) in form (parametrized_name -> (result, additional result)
    *         additional result used in top N metrics, since the result of this metric look like (frequency, value)
    */
  def processAllMetrics(df: DataFrame,
                        colMetrics: Seq[ColumnMetric],
                        fileMetrics: Seq[FileMetric],
                        sourceKeyFields: Seq[String])(
                         implicit settings: DQSettings,
                         sparkContext: SparkContext)
  : (Map[Seq[String], Map[ColumnMetric, (Double, Option[String])]],
    Map[FileMetric, (Double, Option[String])]) = {

    /**
      * Calls a unified metric calculator's constructor
      *
      * @param tag      class tag of calculator
      * @param paramMap parameter map
      * @return instance of metric calculator
      */
    def initGroupCalculator(tag: Class[_],
                            paramMap: ParamMap): MetricCalculator = {
      tag
        .getConstructor(classOf[Map[String, Any]])
        .newInstance(paramMap)
        .asInstanceOf[MetricCalculator]
    }

    /**
      * Applies parametrized metric tail to metric id
      *
      * @param metric input metric
      * @return explicit metric id
      */
    def getParametrizedMetricName(metric: Metric): String = {
      val paramTail = utils.getParametrizedMetricTail(metric.paramMap)
      metric.name + paramTail
    }

    // init file metric calculators
    val fileMetCalculators: Map[FileMetric, MetricCalculator] =
      fileMetrics.map { mm =>
      {
        val calc = mm.name match {
          case "ROW_COUNT" => RowCountMetricCalculator(0) //return rows count
          case x           => throw IllegalParameterException(x)
        }
        mm -> calc
      }
      }.toMap

    /**
      * The main idea of all that construction is calculators grouping.
      *
      * @example You want to obtain multiple quantiles for a specific column,
      *          but calling a new instance of tdigest for each metric isn't effective.
      *
      *          To avoid that first, we're mapping all metric to their calculator classes, then we are
      *          grouping them by column and parameters.
      * @example "FIRST_QUANTILE" for column "A" with parameter "accuracyError"=0.0001
      *          will require an intance of TDigestMetricCalculator. "MEDIAN_VALUE" for column "B" with
      *          the same parameter "accuracyError"=0.0001 will also require an instance of TDigestMetricCalculator.
      *          In our approach the instance will be the same and it will return us results like
      *          Map(("MEDIAN_VALUE:..."->result1),("FIRST_QUANTILE:..."->result2),...)
      *
      *          So in the end we are initializing only unique calculators.
      */
    val metricsByColumn: Map[Seq[String], Seq[ColumnMetric]] =
      colMetrics.groupBy(_.columns)

    val columnsIndexes: Map[String, Int] =
      df.schema.fieldNames.map(s => s -> df.schema.fieldIndex(s)).toMap
    val sourceKeyIds: Seq[Int] = sourceKeyFields.flatMap(i => columnsIndexes.get(i))
    log.info(s"KEY FIELDS: [${sourceKeyFields.mkString(",")}]")
    if (sourceKeyIds.size != sourceKeyFields.size)
      log.warn("Some of key fields were not found! Please, check them.")

    val dumpSize = settings.errorDumpSize

    val groupedCalculators
    : Map[Seq[String], Seq[(MetricCalculator, Seq[ColumnMetric])]] =
      metricsByColumn.map {
        case (colId, metList) =>
          colId -> metList
            .map(mm =>
              (mm,
                initGroupCalculator(MetricMapper.getMetricClass(mm.name), mm.paramMap)))
            .groupBy(_._2)
            .mapValues(_.map(_._1))
            .toSeq
      }

    /**
      * To calculate metrics we are using three-step processing:
      * 1. Iterating over dataframe and passing values to the calculators
      * 2. Updating partition calculators before merging (operations like trimming, shifting, etc)
      * 3. Reducing (merging partition calculator)
      *
      * File and column metrics are storing separately
      */
    val failedRowsForMetric
    : Accumulable[mutable.ArrayBuffer[(String, String)], (String, String)] =
      sparkContext.accumulableCollection(
        mutable.ArrayBuffer.empty[(String, String)])

    val (columnMetricCalculators, fileMetricCalculators): (Map[
      Seq[String],
      Seq[
        (MetricCalculator,
          Seq[
            ColumnMetric])]],
      Map[
        FileMetric,
        MetricCalculator]) =
      df.rdd.treeAggregate((groupedCalculators, fileMetCalculators))(
        seqOp = {
          case ((
            colMetCalcs: Map[Seq[String],
              Seq[(MetricCalculator, Seq[ColumnMetric])]],
            fileMetCalcs: Map[FileMetric, MetricCalculator]
            ),
          row: Row) =>
            val updatedColRes
            : Map[Seq[String], Seq[(MetricCalculator, Seq[ColumnMetric])]] =
              colMetCalcs.map(m => {
                val ids: Seq[Int] = m._1.map(x => columnsIndexes(x))
                val columnValues: Seq[Any] = ids.map(id => row.get(id))

                val incrementedCalculators
                : Seq[(MetricCalculator, Seq[ColumnMetric])] =
                  colMetCalcs(m._1).map {
                    case (calc: MetricCalculator, met: Seq[ColumnMetric]) =>
                      (calc.increment(columnValues), met)
                  }

                (m._1, incrementedCalculators)
              })

            val updatedFileRes: Map[FileMetric, MetricCalculator] = fileMetCalcs
              .map(calc => calc._1 -> calc._2.increment(Seq(row)))

            //refactor if you need error checking without key map
            val failedMetricIds: Iterable[String] =
              updatedColRes.values.flatten.collect {
                case (ic: StatusableCalculator, met: Seq[ColumnMetric])
                  if ic.getStatus == CalculatorStatus.FAILED && ic.getFailCounter < dumpSize =>
                  met.map(_.id)
              }.flatten

            if (failedMetricIds.nonEmpty && sourceKeyIds.nonEmpty) {
              val columnValue =
                sourceKeyIds.map(id => row.get(id)).mkString(",")
              val metIds = failedMetricIds.mkString(",")

              failedRowsForMetric.add((metIds, columnValue))
            }

            (updatedColRes, updatedFileRes)
        },
        combOp = (r, l) => {
          val colMerged
          : Map[Seq[String], Seq[(MetricCalculator, Seq[ColumnMetric])]] =
            l._1.map(c => {
              val zipedCalcs: Seq[((MetricCalculator, Seq[ColumnMetric]),
                (MetricCalculator, Seq[ColumnMetric]))] = r
                ._1(c._1) zip l._1(c._1)
              val merged: Seq[(MetricCalculator, Seq[ColumnMetric])] =
                zipedCalcs.map(zc => (zc._1._1.merge(zc._2._1), zc._1._2))
              (c._1, merged)
            })
          val fileMerged: Map[FileMetric, MetricCalculator] =
            r._2.map(x => x._1 -> l._2(x._1).merge(x._2))
          (colMerged, fileMerged)
        }
      )

    columnMetricCalculators.values.flatten.foreach {
      case (calc: StatusableCalculator, metrics) =>
        log.info(
          s"For metrics:[${metrics.map(_.id).mkString(",")}] were found ${calc.getFailCounter} errors.")
      case (_, _) =>
    }

    settings.errorFolderPath match {
      case Some(_) =>
        log.info(s"Maximum error dump size: $dumpSize")
        val accumulator: mutable.Seq[(Array[String], String)] =
          failedRowsForMetric.value.map {
            case (metIds, errorRow) => (metIds.split(","), errorRow)
          }
        val trimmedAccumulator: Map[String, mutable.Seq[String]] = accumulator
          .flatMap {
            case (met, row) => met.map(m => m -> row)
          }
          .groupBy(_._1)
          .mapValues(_.map(_._2).take(dumpSize))
        trimmedAccumulator.foreach(metErrors =>
          utils.saveErrors(sourceKeyFields, metErrors))
      case None => log.info("No error dump path found")
    }

    /**
      * After processing metrics, there are only results from calculators, not connected with specific
      * metric ids. To do that linking we're generating unique parametrized name of the specific metric in
      * the same way, as they were made in calculators. Then we're combining them together.
      *
      * @note If there are two identical metric, the result will be calculated only once and both metrics will be
      *       linked to it.
      * @example Same condition as in previous example: 2 quanitile metrics over one column with the same
      *          accuracy error. They are evaluated in one calculator. Calculator result is the following:
      *          Map(("MEDIAN_VALUE:..."->result1),("FIRST_QUANTILE:..."->result2),...)
      *          Now, name from calculator and exact metric's name should be the same to be connected.
      */
    // combine file metrics and results
    val fileMetResults: Map[FileMetric, (Double, Option[String])] =
    fileMetricCalculators.map(x => x._1 -> x._2.result()(x._1.name))

    // init list of all metrics per column
    val resultsMap: Map[Seq[String], Map[String, (Double, Option[String])]] =
      columnMetricCalculators.map { colres =>
        val resMap: Map[String, (Double, Option[String])] =
          colres._2.flatMap(calc => calc._1.result()).toMap
        (colres._1, resMap)
      }

    /**
      * Metrics like TOP_N and TOP_N_AGLEBIRD producing multiple results with the different names,
      * template "{metric_name}:{metric_params}_{index}" is used to link in that situation
      */
    // process metrics (SPLIT)
    val processedMetrics: Map[Seq[String], Seq[ColumnMetric]] =
    metricsByColumn.map(col => {

      def splitMetric(baseMetric: ColumnMetric,
                      splitNum: Int): Seq[ColumnMetric] = {
        def generateMetric(bm: ColumnMetric,
                           sn: Int,
                           aggr: Seq[ColumnMetric]): Seq[ColumnMetric] = {
          if (sn > 0) {
            val newMetric = ColumnMetric(
              baseMetric.id + "_" + sn.toString,
              baseMetric.name + "_" + sn.toString,
              baseMetric.description,
              baseMetric.source,
              baseMetric.sourceDate,
              baseMetric.columns,
              baseMetric.paramMap
            )
            return generateMetric(bm, sn - 1, aggr ++ Seq(newMetric))
          }
          aggr
        }

        generateMetric(baseMetric, splitNum, Seq.empty)
      }

      val processed: Seq[ColumnMetric] = col._2.flatMap(metric =>
        metric.name match {
          case "TOP_N" =>
            splitMetric(
              metric,
              metric.paramMap.getOrElse("targetNumber", 10).toString.toInt)
          case _ => Seq(metric)
        })
      (col._1, processed)
    })

    // combine column metrics and results
    val unitedMetricResult
    : Map[Seq[String], Map[ColumnMetric, (Double, Option[String])]] =
      processedMetrics.map(colmet => {
        val resMap: Map[String, (Double, Option[String])] =
          resultsMap(colmet._1)
        val metResMap: Map[ColumnMetric, (Double, Option[String])] = colmet._2
          .map(met => {
            (met,
              resMap.getOrElse(getParametrizedMetricName(met),
                (0.0, Some("not_present"))))
          })
          .toMap
        (colmet._1, metResMap)
      })

    (unitedMetricResult, fileMetResults)
  }

}
