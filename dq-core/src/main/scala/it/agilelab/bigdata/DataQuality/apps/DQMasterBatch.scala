package it.agilelab.bigdata.DataQuality.apps

import java.sql.Connection

import it.agilelab.bigdata.DataQuality.checks.{CheckResult, CheckStatusEnum, LoadCheckResult}
import it.agilelab.bigdata.DataQuality.checks.LoadChecks.{ExeEnum, LoadCheck}
import it.agilelab.bigdata.DataQuality.configs.ConfigReader
import it.agilelab.bigdata.DataQuality.exceptions.IllegalParameterException
import it.agilelab.bigdata.DataQuality.metrics.{ComposedMetricCalculator, _}
import it.agilelab.bigdata.DataQuality.sources.SourceTypes.SourceType
import it.agilelab.bigdata.DataQuality.sources.VirtualSourceProcessor.getActualSources
import it.agilelab.bigdata.DataQuality.sources._
import it.agilelab.bigdata.DataQuality.targets.HdfsTargetConfig
import it.agilelab.bigdata.DataQuality.utils._
import it.agilelab.bigdata.DataQuality.utils.io.db.readers.HBaseLoader
import it.agilelab.bigdata.DataQuality.utils.io.{HdfsReader, HdfsWriter, HistoryDBManager, HiveReader}
import it.agilelab.bigdata.DataQuality.utils.mailing.{NotificationManager, Summary}
import org.apache.hadoop.fs.FileSystem
import org.apache.spark.SparkContext
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.sql.{DataFrame, SQLContext}

import scala.util.{Failure, Success, Try}


object DQMasterBatch extends DQMainClass with DQSparkContext with Logging {

  override protected def body()(implicit fs: FileSystem,
                                sparkContext: SparkContext,
                                sqlContext: SQLContext,
                                sqlWriter: HistoryDBManager,
                                settings: DQSettings): Boolean = {

    /**
      * Configuration file parsing
      */
    log.info(s"[STAGE 1/${settings.stageNum}] Parsing configuration file...")
    log.info("Path: " + settings.configFilePath)
    val configuration = new ConfigReader(settings.configFilePath)

    log.info(s"External database list (size ${configuration.dbConfigMap.size}):")
    configuration.dbConfigMap.par.foreach(x => log.debug(s"  ${x._1} -> ${x._2}"))
    log.info(s"Source list (size ${configuration.sourcesConfigMap.size}):")
    configuration.sourcesConfigMap.par.foreach(x => log.debug(s"  ${x._1} -> ${x._2}"))
    log.info(s"Virtual source list (size ${configuration.virtualSourcesConfigMap.size}):")
    configuration.virtualSourcesConfigMap.par.foreach(x => log.debug(s"  ${x._1} -> ${x._2}"))
    log.info(s"Metrics list (size ${configuration.metricsBySourceList.size}):")
    configuration.metricsBySourceList.par.foreach(x => log.debug(s"  ${x._1} -> ${x._2}"))
    log.info(s"Checks list (size ${configuration.metricsByChecksList.size}):")
    configuration.metricsByChecksList.par.foreach(x => log.debug(s"  ${x._2} -> ${x._1}"))
    log.info(s"Targets list (size ${configuration.targetsConfigMap.size}):")
    configuration.targetsConfigMap.par.foreach(x => log.debug(s"  ${x._1} -> ${x._2}"))

    /**
      * Database connection management
      */
    log.info(s"[STAGE 2/${settings.stageNum}] Connecting to external databases...")
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

    /**
      * Source loading
      */
    log.info(s"[STAGE 3/${settings.stageNum}] Loading data...")
    val (sources: Seq[Source], lcResults: Seq[LoadCheckResult]) = configuration.sourcesConfigMap
      .map {
        case (source, conf) =>
          conf match {
            case hdfsFile: HdfsFile =>
              val loadChecks: Seq[LoadCheck] = configuration.loadChecksMap.getOrElse(source, Seq.empty[LoadCheck])

              val preLoadRes: Seq[LoadCheckResult] =
                loadChecks.filter(_.exeType == ExeEnum.pre).map(x => x.run(Some(hdfsFile))(fs, sqlContext, settings))

              val src: Seq[Source] = HdfsReader
                .load(hdfsFile, settings.ref_date)
                .map(df => Source(source, hdfsFile.date, df, conf.keyfields))

              val postLoadRes: Seq[LoadCheckResult] = loadChecks
                .filter(_.exeType == ExeEnum.post)
                .map(x => x.run(None, Try(src.head.df).toOption)(fs, sqlContext, settings))

              (src, preLoadRes ++ postLoadRes)
            case hiveTableConfig: HiveTableConfig =>
              sqlContext match {
                case hc: HiveContext =>
                  val src: Seq[Source] = HiveReader
                    .loadHiveTable(hiveTableConfig)(hc)
                    .map(df => Source(source, settings.refDateString, df, conf.keyfields))
                  (src, Seq.empty[LoadCheckResult])
                case _ =>
                  throw IllegalParameterException("Hive context wasn't set properly. Check your application.conf")
              }
            case hbConf: HBaseSrcConfig =>
              (
                Seq(Source(source, settings.refDateString, HBaseLoader.loadToDF(hbConf), conf.keyfields)),
                Seq.empty[LoadCheckResult]
              )
            case tableConf: TableConfig =>
              val databaseConfig = configuration.dbConfigMap(tableConf.dbId)
              log.info(s"Loading table ${tableConf.table} from ${tableConf.dbId}")
              val df: DataFrame =
                (tableConf.password, tableConf.password) match {
                  case (Some(u), Some(p)) =>
                    databaseConfig.loadData(tableConf.table, Some(u), Some(p))
                  case _ =>
                    databaseConfig.loadData(tableConf.table)
                }
              (Seq(Source(source, settings.refDateString, df, conf.keyfields)), Seq.empty[LoadCheckResult])
            case x => throw IllegalParameterException(x.getType.toString)
          }
      }
      .foldLeft((Seq.empty[Source], Seq.empty[LoadCheckResult]))((x, y) => (x._1 ++ y._1, x._2 ++ y._2))

    sqlWriter.saveResultsToDB(lcResults.map(_.simplify()), "results_check_load")
    if (sources.length != configuration.sourcesConfigMap.size) {
      val failSrc: Seq[String] = configuration.sourcesConfigMap
        .filterNot(x => sources.map(_.id).toSet.contains(x._1)).map(x =>s"- ${x._1}: ${x._2.getType}").toSeq
      val additional = failSrc.mkString(s"Failed sources: ${failSrc.size}\n","\n","")

      log.error(additional)

      val summary = new Summary(configuration, None, Some(lcResults))
      log.debug(summary.toMailString() + "\n" + additional)

      NotificationManager.sendSummary(summary, Some(additional))
      NotificationManager.saveResultsLocally(summary, None, Some(lcResults))

      return false
    }

    val sourceMap: Map[String, Source] = sources.map(x => (x.id, x)).toMap
    val vsToSave: Set[String]          = configuration.virtualSourcesConfigMap.filter(p => p._2.isSave).keys.toSet
    val virtualSources: Seq[Source]    = getActualSources(configuration.virtualSourcesConfigMap, sourceMap).values.toSeq

    log.info("Saving required sources...")
    virtualSources.foreach(source => {
      (source, settings.vsDumpConfig) match {
        case (src, Some(config)) if vsToSave.contains(src.id) && src.keyfields.nonEmpty =>
          val dataframe = src.df.select(src.keyfields.head, src.keyfields.tail: _*)
          HdfsWriter.saveVirtualSource(dataframe, config.copy(fileName = src.id), settings.refDateString)
          log.info(s"Source ${src.id} was saved with key fields.")
        case (src, Some(config)) if vsToSave.contains(src.id) =>
          HdfsWriter.saveVirtualSource(source.df, config.copy(fileName = src.id), settings.refDateString)
          log.info(s"Virtual source ${src.id} was saved.")
        case (src, _) if vsToSave.contains(src.id) =>
          log.info(s"Virtual source ${src.id} will not be saved.")
        case _ =>
      }
    })

    /**
      * Metrics calculation
      */
    log.info(s"[STAGE 4/${settings.stageNum}] Calculating metrics...")
    val allMetrics: Seq[(String,
                         Map[Seq[String], Map[ColumnMetric, (Double, Option[String])]],
                         Map[FileMetric, (Double, Option[String])])] =
      virtualSources.map(source => {
        log.info(s"Calculating metrics for ${source.id}")

        //select all file metrics to do on this source
        val fileMetrics: Seq[FileMetric] =
          configuration.metricsBySourceMap.getOrElse(source.id, Nil).collect {
            case metric: FileMetric =>
              FileMetric(metric.id, metric.name, metric.description, metric.source, source.date, metric.paramMap)
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
                           metric.paramMap,
                           metric.positions)
          }
        log.info(s"Found column metrics: ${colMetrics.size}")

        if (fileMetrics.isEmpty && colMetrics.isEmpty) {
          (source.id,
           Map
             .empty[Seq[String], Map[ColumnMetric, (Double, Option[String])]],
           Map.empty[FileMetric, (Double, Option[String])] empty)
        } else {
          //compute all metrics
          val results: (Map[Seq[String], Map[ColumnMetric, (Double, Option[String])]],
                        Map[FileMetric, (Double, Option[String])]) =
            MetricProcessor.processAllMetrics(source.df, colMetrics, fileMetrics, source.keyfields)

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
    log.info(s"[STAGE 5/${settings.stageNum}] Calculating composed metrics...")
    // todo: It's possible to calculate composed using $primitiveMetricResults as zero value to avoid extra merges
    val composedMetricResults: Seq[ComposedMetricResult] =
      configuration.composedMetrics
        .foldLeft[Seq[ComposedMetricResult]](Seq.empty[ComposedMetricResult])((accum, curr) => {
          log.info(s"Calculating ${curr.id} with formula ${curr.formula}")
          val composedMetricCalculator =
            new ComposedMetricCalculator(primitiveMetricResults ++ accum)
          val currRes: ComposedMetricResult =
            composedMetricCalculator.run(curr)
          accum ++ Seq(currRes)
        })

    val allMetricResults: Seq[MetricResult] = primitiveMetricResults ++ composedMetricResults

    /**
      * DEFINE and PERFORM CHECKS
      */
    log.info(s"[STAGE 6/${settings.stageNum}] Performing checks...")
    val buildChecks = configuration.metricsByCheckMap.map {
      case (check, metricList) =>
        val resList = metricList.flatMap { mId =>
          val ll = allMetricResults.filter(_.metricId == mId)
          if (ll.size == 1) Option(ll.head) else None
        }
        check.addMetricList(resList)
    }.toSeq

    val createdChecks = buildChecks.map(cmr => s"${cmr.id}, ${cmr.getMetrics} - ${cmr.getDescription}")
    log.info(s" * Checks created: ${createdChecks.size}")
    createdChecks.foreach(str => log.info(str))

    val checkResults: Seq[CheckResult] = buildChecks
      .flatMap { e =>
        Try {
          e.run
        } match {
          case Success(res) => Some(res)
          case Failure(e) => None
        }
      }

    log.info(s"Check Results:")
    checkResults.foreach(cr => log.info("  " + cr.message))

    /**
      * PERFORM SQL CHECKS
      */
    log.info(s"[STAGE 7/${settings.stageNum}] Performing SQL checks...")
    val sqlCheckResults: List[CheckResult] =
      configuration.sqlChecksList.map(check => {
        log.info("Calculating " + check.id + " " + check.description)
        check.executeCheck(dbConnections(check.source))
      })

    // Closing db connections
    dbConnections.values.foreach(_.close())

    val finalCheckResults: Seq[CheckResult] = checkResults ++ sqlCheckResults

    log.info(s"[STAGE 8/${settings.stageNum}] Processing results...")
    log.info(s"Saving results to the database...")
    log.info(s"With reference date: ${settings.refDateString}")
    sqlWriter.saveResultsToDB(colMetricResultsList, "results_metric_columnar")
    sqlWriter.saveResultsToDB(fileMetricResultsList, "results_metric_file")
    sqlWriter.saveResultsToDB(composedMetricResults, "results_metric_composed")
    sqlWriter.saveResultsToDB(finalCheckResults, "results_check")

    val targetResultMap: Map[String, Seq[Product with Serializable with TypedResult]] = Map(
      settings.backComp.fileMetricTargetType -> fileMetricResultsList,
      settings.backComp.columnMetricTargetType -> colMetricResultsList,
      settings.backComp.composedMetricTargetType -> composedMetricResults,
      settings.backComp.checkTargetType -> finalCheckResults,
      settings.backComp.loadCheckTargetType -> lcResults
    )

    log. info("Saving targets...")
    configuration.targetsConfigMap.foreach(tar =>
      tar._1 match {
        case "SYSTEM" =>
          tar._2.foreach(conf => HdfsWriter.processSystemTarget(conf, finalCheckResults))
        case _ =>
          tar._2.foreach(conf => HdfsWriter.save(conf.asInstanceOf[HdfsTargetConfig], targetResultMap(tar._1)))
    })

    log.info(s"[STAGE 9/${settings.stageNum}] Postprocessing...")
    val vsHdfs: Set[HdfsFile] = settings.vsDumpConfig match {
      case Some(conf) =>
        vsToSave.map(vs => {
          val fileName = conf.path + "/" + vs + "." + conf.fileFormat //-${targetConfig.subType}
          HdfsFile.apply(vs, fileName, conf.fileFormat, true, settings.refDateString)
        })
      case _ => Set.empty[HdfsFile]
    }

    configuration.getPostprocessors.foldLeft(vsHdfs)((files, pp) =>
      files.+(pp.process(files, allMetricResults, finalCheckResults)))

    log.info(s"[STAGE 10/${settings.stageNum}] Saving summary files and mailing reports...")
    val summary = new Summary(configuration, Some(checkResults), Some(lcResults))
    log.debug(summary.toMailString())
    NotificationManager.saveResultsLocally(summary, Some(checkResults), Some(lcResults))
    NotificationManager.sendSummary(summary)

    true
  }
}
