package it.agilelab.bigdata.DataQuality.apps

import java.sql.Connection

import it.agilelab.bigdata.DataQuality.checks.CheckResult
import it.agilelab.bigdata.DataQuality.configs.ConfigReader
import it.agilelab.bigdata.DataQuality.exceptions.IllegalParameterException
import it.agilelab.bigdata.DataQuality.metrics.{ComposedMetricCalculator, _}
import it.agilelab.bigdata.DataQuality.sources.VirtualSourceProcessor.getActualSources
import it.agilelab.bigdata.DataQuality.sources._
import it.agilelab.bigdata.DataQuality.targets.HdfsTargetConfig
import it.agilelab.bigdata.DataQuality.utils._
import it.agilelab.bigdata.DataQuality.utils.io.db.readers.HBaseLoader
import it.agilelab.bigdata.DataQuality.utils.io.{HdfsReader, HdfsWriter, HiveReader, LocalDBManager}
import org.apache.hadoop.fs.FileSystem
import org.apache.spark.SparkContext
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.sql.{DataFrame, Row, SQLContext}

import scala.util.{Failure, Success, Try}

/**
  * Created by Gianvito Siciliano on 30/01/2017.
  */
object DQMasterBatch extends DQMainClass with DQSparkContext with Logging {

  override protected def body()(implicit fs: FileSystem,
                                sparkContext: SparkContext,
                                sqlContext: SQLContext,
                                sqlWriter: LocalDBManager,
                                settings: DQSettings): Boolean = {

    /**
      * PARSE CONFIGURATION FILE
      *
      */
    val configuration = new ConfigReader(settings.configFilePath)

    log.info("\n EXTERNAL DATABASES:")
    log.info(configuration.dbConfigMap.mkString(" \n "))
    log.info("\n SOURCES:")
    log.info(configuration.sourcesConfigMap.mkString(" \n "))
    log.info("\n METRICS:")
    log.info(configuration.metricsBySourceList.mkString(" \n "))
    log.info("\n CHECKS:")
    log.info(configuration.metricsByChecksList.mkString(" \n "))
    log.info("\n TARGETS:")
    log.info(configuration.targetsConfigMap.mkString(" \n "))

    /**
      * LOAD SOURCES
      */
    log.info(s"\n# Connecting to external databases...")
    val dbConnections: Map[String, Connection] =
      configuration.dbConfigMap.flatMap({ db =>
        log.info("Trying to connect to " + db._1)
        val res = Try(db._1 -> db._2.getConnection).toOption
        res match {
          case Some(_) => log.info("Connection successful")
          case None    => log.warn("Connection failed")
        }
        res
      })

    log.info(s"\n# Loading data...")
    val sources: Seq[Source] = configuration.sourcesConfigMap
      .map {
        case (source, conf) =>
          //        val keyFieldOpt=if(conf.keyfields) Some(conf.keyfields) else None
          conf match {
            case hdfsFile: HdfsFile =>
              HdfsReader
                .load(hdfsFile, settings.ref_date)
                .map(df => Source(source, hdfsFile.date, df, conf.keyfields))
            case hiveTableConfig: HiveTableConfig =>
              sqlContext match {
                case hc: HiveContext =>
                  HiveReader.loadHiveTable(hiveTableConfig)(hc).map(df =>
                    Source(source, settings.refDateString, df, conf.keyfields))
                case _ =>
                  throw IllegalParameterException("Hive context wasn't set properly. Check your application.conf")
              }
            case hbConf: HBaseSrcConfig =>
              Seq(
                Source(source,
                       settings.refDateString,
                       HBaseLoader.loadToDF(hbConf),
                       conf.keyfields))
            case outputFile: OutputFile =>
              val output = HdfsReader
                .loadOutput(outputFile)
                .map(t => Source(t._1, outputFile.date, t._2, conf.keyfields))
              output.foreach(log warn _)
              output
                .find(_.id == "DWH_SWIFT_ROWS")
                .foreach(_.df.collect().foreach(log warn _))
              output
            case tableConf: TableConfig =>
              val databaseConfig = configuration.dbConfigMap(tableConf.dbId)
              log.info(
                s"Loading table ${tableConf.table} from ${tableConf.dbId}")
              val df: DataFrame =
                (tableConf.password, tableConf.password) match {
                  case (Some(u), Some(p)) =>
                    databaseConfig.loadData(tableConf.table, Some(u), Some(p))
                  case _ =>
                    databaseConfig.loadData(tableConf.table)
                }
              Seq(Source(source, settings.refDateString, df, conf.keyfields))
            case x => throw IllegalParameterException(x.getType.toString)
          }
      }
      .toSeq
      .flatten

    val sourceMap: Map[String, Source] = sources.map(x => (x.id, x)).toMap
    val vsToSave: Set[String] =
      configuration.virtualSourcesConfigMap.filter(p => p._2.isSave).keys.toSet
    val virtualSources: Seq[Source] = getActualSources(
      configuration.virtualSourcesConfigMap,
      sourceMap).values.toSeq

    /**
      * CALCULATE METRICS
      */
    log.info(s"\n# Starting metrics processing...")
    val allMetrics
      : Seq[(String,
             Map[Seq[String], Map[ColumnMetric, (Double, Option[String])]],
             Map[FileMetric, (Double, Option[String])])] =
      virtualSources.map(source => {

        (source, settings.vsDumpConfig) match {
          case (src, Some(config))
              if vsToSave.contains(src.id) && src.keyfields.nonEmpty =>
            val dataframe =
              src.df.select(src.keyfields.head, src.keyfields.tail: _*)
            HdfsWriter.saveVirtualSource(dataframe,
                                         config.copy(fileName = src.id),
                                         settings.refDateString)
            log.info(s"Source ${src.id} was saved with key fields.")
          case (src, Some(config)) if vsToSave.contains(src.id) =>
            HdfsWriter.saveVirtualSource(source.df,
                                         config.copy(fileName = src.id),
                                         settings.refDateString)
            log.info(s"Source ${src.id} was saved.")
          case (src, _) if vsToSave.contains(src.id) =>
            log.info(s"Source ${src.id} will not be saved.")
          case _ =>
        }

        log.info(s"Calculating metrics for ${source.id}")

        //select all file metrics to do on this source
        val fileMetrics: Seq[FileMetric] =
          configuration.metricsBySourceMap.getOrElse(source.id, Nil).collect {
            case metric: FileMetric =>
              FileMetric(metric.id,
                         metric.name,
                         metric.description,
                         metric.source,
                         source.date,
                         metric.paramMap)
          }
        log.info(s"Found file metrics: ${fileMetrics.size}")

        //select all columnar metrics to do on this source
        val colMetrics =
          configuration.metricsBySourceMap.getOrElse(source.id, Nil).collect {
            case metric: ColumnMetric =>
              ColumnMetric(metric.id,
                           metric.name,
                           metric.description,
                           metric.source,
                           source.date,
                           metric.columns,
                           metric.paramMap)
          }
        log.info(s"Found column metrics: ${colMetrics.size}")

        if (fileMetrics.isEmpty && colMetrics.isEmpty) {
          (source.id,
           Map
             .empty[Seq[String], Map[ColumnMetric, (Double, Option[String])]],
           Map.empty[FileMetric, (Double, Option[String])] empty)
        } else {
          //compute all metrics
          val results
            : (Map[Seq[String], Map[ColumnMetric, (Double, Option[String])]],
               Map[FileMetric, (Double, Option[String])]) =
            MetricProcessor.processAllMetrics(source.df,
                                              colMetrics,
                                              fileMetrics,
                                              source.keyfields)

          source.df.unpersist()

          (source.id, results._1, results._2)
        }
      })

    /**
      * CREATE METRIC RESULTS (for checks)
      */
    val colMetricResultsList: Seq[ColumnMetricResult] =
      allMetrics.flatMap {
        case (_, resultsOnColumns, _) =>
          resultsOnColumns.flatMap {
            case (colIds, metricResultsMap) =>
              metricResultsMap.map { mr =>
                ColumnMetricResult(
                  mr._1.id,
                  mr._1.sourceDate,
                  mr._1.name,
                  mr._1.source,
                  colIds,
                  mapToJsonString(mr._1.paramMap),
                  mr._2._1,
                  mr._2._2.getOrElse("")
                )
              }
          }.toList
      }

    val fileMetricResultsList: Seq[FileMetricResult] =
      allMetrics.flatMap {
        case (_, _, resultsOnfile) =>
          resultsOnfile.map {
            case (fileMetric, metricCalc) =>
              FileMetricResult(
                fileMetric.id,
                fileMetric.sourceDate,
                fileMetric.name,
                fileMetric.source,
                metricCalc._1,
                metricCalc._2.getOrElse("")
              )
          }
      }

    val primitiveMetricResults = fileMetricResultsList ++ colMetricResultsList

    /**
      * CALCULATING COMPOSED METRICS
      */
    log.info(s"\n# Calculating composed metrics...")

    // todo: It's possible to calculate composed using $primitiveMetricResults as zero value to avoid extra merges
    val composedMetricResults: Seq[ComposedMetricResult] =
      configuration.composedMetrics
        .foldLeft[Seq[ComposedMetricResult]](Seq.empty[ComposedMetricResult])(
          (accum, curr) => {
            log.info(s"Calculating ${curr.id} with formula ${curr.formula}")
            val composedMetricCalculator =
              new ComposedMetricCalculator(primitiveMetricResults ++ accum)
            val currRes: ComposedMetricResult =
              composedMetricCalculator.run(curr)
            accum ++ Seq(currRes)
          })

    val allMetricResults
      : Seq[MetricResult] = primitiveMetricResults ++ composedMetricResults

    /**
      * DEFINE and PERFORM CHECKS
      */
    val buildChecks = configuration.metricsByCheckMap.map {
      case (check, metricList) =>
        val resList = metricList.flatMap { mId =>
          val ll = allMetricResults.filter(_.metricId == mId)
          if (ll.size == 1) Option(ll.head) else None
        }
        check.addMetricList(resList)
    }.toSeq

    val createdChecks = buildChecks.map(cmr =>
      s"${cmr.id}, ${cmr.getMetrics} - ${cmr.getDescription}")
    log.info(s"\n# Checks created... ${createdChecks.size}")
    createdChecks.foreach(str => log.info(str))

    val checkResults: Seq[CheckResult] = buildChecks
      .flatMap { e =>
        Try {
          e.run
        } match {
          case Success(res) => Some(res)
          case Failure(e) => {
            log.error(e.getMessage)
            None
          }
        }
      }

    log.info(s"\n# Check Results...")
    checkResults.foreach(cr => log.info(cr.message))

    /**
      * PERFORM SQL CHECKS
      */
    log.info(s"\n# SQL checks processing...")

    val sqlCheckResults: List[CheckResult] =
      configuration.sqlChecksList.map(check => {
        log.info("Calculating " + check.id + " " + check.description)
        check.executeCheck(dbConnections(check.source))
      })

    // Closing db connections
    dbConnections.values.foreach(_.close())

    /**
      * SAVE RESULTS
      */
    val finalCheckResults: Seq[CheckResult] = checkResults ++ sqlCheckResults

    log.info(s"\n# Saving results to the database...")
    log.info(s"With reference date: ${settings.refDateString}")
    sqlWriter.saveResultsToDB(colMetricResultsList, "results_metric_columnar")
    sqlWriter.saveResultsToDB(fileMetricResultsList, "results_metric_file")
    sqlWriter.saveResultsToDB(composedMetricResults, "results_metric_composed")
    sqlWriter.saveResultsToDB(finalCheckResults, "results_check")

    val targetResultMap
      : Map[String, Seq[Product with Serializable with TypedResult]] = Map(
      "COLUMNAR-METRICS" -> colMetricResultsList,
      "FILE-METRICS" -> fileMetricResultsList,
      "CHECKS" -> finalCheckResults,
      "COMPOSED-METRICS" -> composedMetricResults
    )

    log.info(s"\n# Targets processing...")
    configuration.targetsConfigMap.foreach(tar =>
      tar._1 match {
        case "SYSTEM" =>
          tar._2.foreach(conf =>
            HdfsWriter.processSystemTarget(conf, finalCheckResults))
        case _ =>
          tar._2.foreach(
            conf =>
              HdfsWriter.save(conf.asInstanceOf[HdfsTargetConfig],
                              targetResultMap(tar._1)))
    })

    log.info(s"\n# Starting postprocessing...")

    val vsHdfs: Set[HdfsFile] = settings.vsDumpConfig match {
      case Some(conf) =>
        vsToSave.map(vs => {
          val fileName = conf.path + "/" + vs + "." + conf.fileFormat //-${targetConfig.subType}
          HdfsFile(vs,
                   fileName,
                   conf.fileFormat,
                   conf.delimiter,
                   true,
                   settings.refDateString)
        })
      case _ => Set.empty[HdfsFile]
    }

    configuration.getPostprocessors.foldLeft(vsHdfs)((files, pp) =>
      files.+(pp.process(files, allMetricResults, finalCheckResults)))

    true
  }
}
