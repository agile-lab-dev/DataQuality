package it.agilelab.bigdata.DataQuality.apps

import it.agilelab.bigdata.DataQuality.checks.CheckResult
import it.agilelab.bigdata.DataQuality.configs.ConfigReader
import it.agilelab.bigdata.DataQuality.exceptions.IllegalParameterException
import it.agilelab.bigdata.DataQuality.metrics.SourceProcessor.ColumnId
import it.agilelab.bigdata.DataQuality.metrics._
import it.agilelab.bigdata.DataQuality.sources.{HdfsFile, Source}
import it.agilelab.bigdata.DataQuality.utils._
import it.agilelab.bigdata.DataQuality.utils.io.{HdfsReader, HdfsWriter}
import org.apache.hadoop.fs.FileSystem
import org.apache.spark.SparkContext
import org.apache.spark.sql.SQLContext

import scala.util.{Failure, Success, Try}

/**
  * Created by Gianvito Siciliano on 30/01/2017.
  */

object DQMasterBatch
    extends DQMainClass
      with DQSparkContext
      with Logging
      {

  override protected def body()(implicit fs: FileSystem, sparkContext: SparkContext, sqlContext: SQLContext, settings: DQSettings): Boolean = {

    /**
      * PARSE CONFIGURATION FILE
      *
      */
    val configuration = new ConfigReader(settings.configFilePath)

    log.info("\n")
    log.info("\n SOURCES:")
    log.info(configuration.sourcesConfigMap.mkString(" \n "))
    log.info("\n")
    log.info("\n METRICS:")
    log.info(configuration.metricsBySourceList.mkString(" \n "))
    log.info("\n")
    log.info("\n CHECKS:")
    log.info(configuration.metricsByChecksList.mkString(" \n "))
    log.info("\n")
    log.info("\n TARGETS:")
    log.info(configuration.targetsConfigMap.mkString(" \n "))
    log.info("\n\n")




    /**
      * LOAD SOURCES
      */

    val sources: Seq[Source] = configuration.sourcesConfigMap.map{ s =>
      s._2.getType match {
        case "HDFS" => {
          val optDF = HdfsReader.load(s._2.getType.asInstanceOf[HdfsFile], settings.ref_date)
          if(optDF.isDefined)
            Option(Source(s._1, optDF.get))
          else
            None
        }
        case x => throw new IllegalParameterException(x)
      }
    }.toSeq.flatten


    /**
      * CALCULATE METRICS
      */
    val allMetrics = sources.map(source => {

      //select all columnar metrics to do on this source
      val colMetrics = configuration.metricsBySourceMap(source.id)
        .collect{ case metric: ColumnMetric => metric}

      //compute columnar metrics
      val resultsOnColumns: Map[ColumnId, Map[ColumnMetric, MetricCalculator]] =
        SourceProcessor
          .processColumnMetrics(source.df, colMetrics)

      //select all file metrics to do on this source
      val fileMetrics: Seq[FileMetric] = configuration.metricsBySourceMap(source.id)
        .collect{ case metric: FileMetric => metric}

      //compute file metrics
      val resultsOnFile: Map[FileMetric, MetricCalculator] =
        SourceProcessor
          .processFileMetrics(source.df, fileMetrics)

      source.df.unpersist()

      (source.id, resultsOnColumns, resultsOnFile)
    })

    //logs metrics
    log.info(s"\n# Metric Results...")
    val colMetricsToLog  = allMetrics.flatMap(s => s._2.flatMap(m => m._2.map( mm => s"Metric_${mm._1.id} on file ${s._1} - ${m._1} - ${mm._1.name} (${mm._1.paramMap.mkString(", ")}): ${mm._2.result()}")))
    val fileMetricsToLog = allMetrics.flatMap(s => s._3.map(mm => s"Metric_${mm._1.id} on file ${s._1} - ${mm._1.name} (${mm._1.paramMap.mkString(", ")}): ${mm._2.result()}"))
    (colMetricsToLog ++ fileMetricsToLog) foreach(str => log.info(str) )



    /**
     * CREATE METRIC RESULTS (for checks)
     */
    val colMetricResultsList: Seq[ColumnMetricResult] =
      allMetrics.flatMap{ case (fileId, resultsOnColumns, resultsOnfile) =>
        resultsOnColumns.flatMap{ case (colId, metricResultsMap) =>
          metricResultsMap.map{ mr =>
            ColumnMetricResult(
              mr._1.id,
              mr._1.name,
              mr._1.source,
              colId,
              mr._2.result()
            )
          }
        }.toList
      }

    val fileMetricResultsList: Seq[FileMetricResult] =
      allMetrics.flatMap{ case (fileId, resultsOnColumns, resultsOnfile) =>
        resultsOnfile.map { case (fileMetric, metricCalc) =>
          FileMetricResult(
            fileMetric.id,
            fileMetric.name,
            fileMetric.source,
            metricCalc.result()
          )
        }
      }

    val allMetricResults = fileMetricResultsList ++ colMetricResultsList


    /**
      * DEFINE and PERFORM CHECKS
      */
    val buildChecks = configuration.metricsByCheckMap.map{ case (check, metricList) =>
      val resList = metricList.flatMap { mId =>
        val ll = allMetricResults.filter(_.id == mId)
        if (ll.size == 1) Option(ll.head) else None
      }
      check.addMetricList(resList)
    }.toSeq

    val createdChecks = buildChecks.map(cmr => s"${cmr.id}, ${cmr.getMetrics} - ${cmr.getDescription}")
    log.info(s"\n# Checks created... ${createdChecks.size}")
    createdChecks.foreach(str => log.info(str))

    val checkResults: Seq[CheckResult] = buildChecks
      .flatMap { e =>
        Try {
          e.run
        } match {
          case Success(res)=> Some(res)
          case Failure(e) => {
            log.error(e.getMessage)
            None
          }
        }
      }

    log.info(s"\n# Check Results...")
    checkResults.foreach( cr => log.info(cr.message))


    /**
      * SAVE CHECK RESULTS
      */
    val colMetricDF  = sqlContext.createDataFrame(colMetricResultsList)
    val fileMetricDF = sqlContext.createDataFrame(fileMetricResultsList)
    val checkDF      = sqlContext.createDataFrame(checkResults)

    colMetricDF.show()
    fileMetricDF.show()
    checkDF.show()

    HdfsWriter.save(configuration.targetsConfigMap("COLUMNAR-METRICS"), colMetricDF)
    HdfsWriter.save(configuration.targetsConfigMap("FILE-METRICS"), fileMetricDF)
    HdfsWriter.save(configuration.targetsConfigMap("CHECKS"), checkDF)




    true
  }

}