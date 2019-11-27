package it.agilelab.bigdata.DataQuality.configs

import java.io.File
import java.util
import java.util.Map.Entry

import com.typesafe.config.{Config, ConfigFactory, ConfigObject, ConfigValue}
import it.agilelab.bigdata.DataQuality.checks.Check
import it.agilelab.bigdata.DataQuality.checks.LoadChecks.{LoadCheck, LoadCheckEnum}
import it.agilelab.bigdata.DataQuality.checks.SQLChecks.SQLCheck
import it.agilelab.bigdata.DataQuality.checks.SnapshotChecks._
import it.agilelab.bigdata.DataQuality.checks.TrendChecks._
import it.agilelab.bigdata.DataQuality.exceptions.{IllegalParameterException, MissingParameterInException}
import it.agilelab.bigdata.DataQuality.metrics.MetricProcessor.ParamMap
import it.agilelab.bigdata.DataQuality.metrics._
import it.agilelab.bigdata.DataQuality.postprocessors.{BasicPostprocessor, PostprocessorType}
import it.agilelab.bigdata.DataQuality.sources._
import it.agilelab.bigdata.DataQuality.targets.{HdfsTargetConfig, SystemTargetConfig, TargetConfig}
import it.agilelab.bigdata.DataQuality.utils.io.HistoryDBManager
import it.agilelab.bigdata.DataQuality.utils.{DQSettings, Logging, generateMetricSubId}
import org.apache.spark.storage.StorageLevel
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

import scala.collection.JavaConversions._
import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.util.Try

class ConfigReader(configNameFile: String)(implicit sqlWriter: HistoryDBManager, settings: DQSettings) extends Logging {

  /**
    * Parsed config OBJECTS
    *
    */
  //load conf file
  val configObj: Config = ConfigFactory.parseFile(new File(configNameFile)).resolve()

  val dbConfigMap: Map[String, DatabaseConfig] = getDatabasesById

  //parse sources, metrics, checks, it.agilelab.bigdata.targets
  val sourcesConfigMap: Map[String, SourceConfig]       = getSourcesById
  val virtualSourcesConfigMap: Map[String, VirtualFile] = getVirtualSourcesById

  lazy val loadChecksMap: Map[String, Seq[LoadCheck]] = getLoadChecks

  val metricsBySourceList: List[(String, Metric)]        = getMetricsBySource
  lazy val metricsBySourceMap: Map[String, List[Metric]] = metricsBySourceList.groupBy(_._1).mapValues(_.map(_._2))

  val metricsByChecksList: List[(Check, String)]       = getMetricByCheck
  lazy val metricsByCheckMap: Map[Check, List[String]] = metricsByChecksList.groupBy(_._1).mapValues(_.map(_._2))

  lazy val composedMetrics: List[ComposedMetric] = getComposedMetrics

  lazy val targetsConfigMap: Map[String, List[TargetConfig]] = getTargetsConfigMap

  lazy val sqlChecksList: List[SQLCheck] = getSqlChecks

  lazy val getPostprocessors: Seq[BasicPostprocessor] = getPostprocessorConfig

  /**
    * Config PARSER
    *  - DQ Databases
    *  - DQ Sources
    *  - DQ Metrics
    *  - DQ Checks
    *  - DQ ComposedMetrics
    *  - DQ Targets
    *
    */
  /**
    * Parses sources from configuration file
    * @return Map of (source_id, source_config)
    */
  private def getSourcesById: Map[String, SourceConfig] = {
    val sourcesList: List[ConfigObject] =
      configObj.getObjectList("Sources").toList

    def parseDateFromPath(path: String): String = {

      val length = path.length
      val sub    = path.substring(length - 8, length) // YYYYMMDD

      val formatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyyMMdd")
      val outputFormat                 = DateTimeFormat.forPattern("yyyy-MM-dd")

      formatter.parseDateTime(sub).toString(outputFormat)
    }

    sourcesList.map { src =>
      val generalConfig = src.toConfig
      val keyFieldList: scala.Seq[String] =
        if (generalConfig.hasPath("keyFields"))
          generalConfig.getStringList("keyFields")
        else Seq.empty
      generalConfig.getString("type") match {
        case "HDFS" =>
          val id       = generalConfig.getString("id")
          val path     = generalConfig.getString("path")
          val fileType = generalConfig.getString("fileType")

          val header = Try(generalConfig.getBoolean("header")).getOrElse(false)

          val delimiter = Try(generalConfig.getString("delimiter")).toOption
          val quote     = Try(generalConfig.getString("quote")).toOption
          val escape    = Try(generalConfig.getString("escape")).toOption

          val date = Try(parseDateFromPath(path))
            .getOrElse(Try(generalConfig.getString("date")).getOrElse(settings.refDateString))

          val schema = generalConfig.getString("fileType") match {
            case "fixed" =>
              if (Try(generalConfig.getAnyRef("schema")).isSuccess)
                getFixedStructSchema(generalConfig)
              else if (Try(generalConfig.getStringList("fieldLengths")).isSuccess)
                getFixedSchema(generalConfig)
              else {
                val allKeys = generalConfig.entrySet().map(_.getKey)
                throw IllegalParameterException("\n CONFIG: " + allKeys.mkString(" - "))
              }
            case "csv"     => getStructSchema(generalConfig)
            case "avro"    => getStructSchema(generalConfig)
            case "parquet" => getStructSchema(generalConfig)
            case x         => throw IllegalParameterException(x)
          }

          id -> HdfsFile(id, path, fileType, header, date, delimiter, quote, escape, schema, keyFieldList)
        case "OUTPUT" =>
          val path = generalConfig.getString("path")
          "OUTPUT" -> OutputFile("OUTPUT", path, "csv", Some("|"), true, "*")
        case "TABLE" =>
          val id         = generalConfig.getString("id")
          val databaseId = generalConfig.getString("database")
          val table      = generalConfig.getString("table")
          val username   = Try { generalConfig.getString("username") }.toOption
          val password   = Try { generalConfig.getString("password") }.toOption

          id -> TableConfig(id, databaseId, table, username, password, keyFieldList)
        case "HIVE" =>
          val id    = generalConfig.getString("id")
          val date  = generalConfig.getString("date")
          val query = generalConfig.getString("query")

          id -> HiveTableConfig(id, date, query, keyFieldList)
        case "HBASE" =>
          val id        = generalConfig.getString("id")
          val table     = generalConfig.getString("table")
          val hbColumns = generalConfig.getStringList("columns")
          id -> HBaseSrcConfig(id, table, hbColumns)
        case x => throw IllegalParameterException(x)
      }
    }.toMap
  }

  private def getVirtualSourcesById: Map[String, VirtualFile] = {

    if (configObj.hasPath("VirtualSources")) {
      val sourcesList: List[ConfigObject] =
        configObj.getObjectList("VirtualSources").toList

      sourcesList.map { src =>
        val generalConfig = src.toConfig
        val keyFieldList: scala.Seq[String] =
          if (generalConfig.hasPath("keyFields"))
            generalConfig.getStringList("keyFields")
          else Seq()

        val parentSourcesIds: scala.Seq[String] =
          generalConfig.getStringList("parentSources")

        val isSave: Boolean = Try { generalConfig.getBoolean("save") }.toOption
          .getOrElse(false)
        val id = generalConfig.getString("id")
        generalConfig.getString("type") match {
          case "FILTER-SQL" =>
            val sql = generalConfig.getString("sql")
            val persist: Option[StorageLevel] =
              if (generalConfig.hasPath("persist"))
                Some(StorageLevel.fromString(generalConfig.getString("persist")))
              else None
            id -> VirtualFileSelect(id, parentSourcesIds, sql, keyFieldList, isSave, persist)
          case "JOIN-SQL" =>
            val sql = generalConfig.getString("sql")
            val persist: Option[StorageLevel] =
              if (generalConfig.hasPath("persist"))
                Some(StorageLevel.fromString(generalConfig.getString("persist")))
              else None
            id -> VirtualFileJoinSql(id, parentSourcesIds, sql, keyFieldList, isSave, persist)
          case "JOIN" =>
            val joiningColumns = generalConfig.getStringList("joiningColumns")
            val joinType       = generalConfig.getString("joinType")
            id -> VirtualFileJoin(id, parentSourcesIds, joiningColumns, joinType, keyFieldList, isSave)

          case x => throw IllegalParameterException(x)
        }
      }.toMap
    } else {
      Map.empty
    }

  }

  /**
    * Parses databases from configuration file
    * @return Map of (db_id, db_config)
    */
  private def getDatabasesById: Map[String, DatabaseConfig] = {
    val dbList: List[ConfigObject] = Try {
      configObj.getObjectList("Databases").toList
    }.getOrElse(List.empty)

    dbList.map { db =>
      val generalConfig = db.toConfig
      val outerConfig   = generalConfig.getConfig("config")
      val id            = generalConfig.getString("id")
      val subtype       = generalConfig.getString("subtype")
      val host          = outerConfig.getString("host")

      val port     = Try(outerConfig.getString("port")).toOption
      val service  = Try(outerConfig.getString("service")).toOption
      val user     = Try(outerConfig.getString("user")).toOption
      val password = Try(outerConfig.getString("password")).toOption
      val schema   = Try(outerConfig.getString("schema")).toOption

      id -> DatabaseConfig(id, subtype, host, port, service, user, password, schema)
    }.toMap
  }

  /**
    * Parses metrics from configuration file
    * @return Map of (file_id, metric)
    */
  private def getMetricsBySource: List[(String, Metric)] = {
    val metricsList: List[ConfigObject] =
      configObj.getObjectList("Metrics").toList

    val metricFileList: List[(String, Metric)] = metricsList.map { mts =>
      val outerConf  = mts.toConfig
      val metricType = outerConf.getString("type")
      val id         = outerConf.getString("id")
      val name       = outerConf.getString("name")
      val descr      = outerConf.getString("description")
      val intConfig  = outerConf.getObject("config").toConfig
      val params     = getParams(intConfig)
      val applyFile  = intConfig.getString("file")

      metricType match {
        case "COLUMN" =>
          val applyColumns = intConfig.getStringList("columns")
          val columnPos: scala.Seq[Int] =
            Try(intConfig.getIntList("positions").toSeq.map(_.toInt)).toOption
              .getOrElse(Seq.empty)
          applyFile -> ColumnMetric(id, name, descr, applyFile, "", applyColumns, params, columnPos)
        case "FILE" =>
          applyFile -> FileMetric(id, name, descr, applyFile, "", params)
        case x => throw IllegalParameterException(x)
      }
    }

    metricFileList
  }

  /**
    * Parses sql checks from configuration file
    * @return List of SQL checks
    */
  private def getSqlChecks: List[SQLCheck] = {
    val checkList: List[ConfigObject] = configObj.getObjectList("Checks").toList
    val sqlChecks = checkList.flatMap { check =>
      val outerConf = check.toConfig
      val checkType = outerConf.getString("type")
      checkType match {
        case "sql" =>
          val description =
            Try {
              outerConf.getString("description")
            }.toOption
          val subtype   = outerConf.getString("subtype")
          val innerConf = outerConf.getConfig("config")
          val source    = innerConf.getString("source")
          val query     = innerConf.getString("query")
          val id = Try {
            outerConf.getString("id")
          }.toOption.getOrElse(subtype + ":" + checkType + ":" + source + ":" + query.hashCode)
          val date = Try {
            outerConf.getString("date")
          }.toOption.getOrElse(settings.refDateString)
          val sourceConf: DatabaseConfig = this.dbConfigMap(source) // "ORACLE"
          List(SQLCheck(id, description.getOrElse(""), subtype, source, sourceConf, query, date))
        case "snapshot" | "trend" => List.empty
        case x                    => throw IllegalParameterException(x)
      }
    }
    sqlChecks
  }

  /**
    * Parses checks connected with metric from configuration file
    * @return List of (Check, MetricId)
    */
  private def getMetricByCheck: List[(Check, String)] = {
    val checksList: List[ConfigObject] = configObj.getObjectList("Checks").toList

    val metricListByCheck = checksList.flatMap { chks =>
      val outerConf = chks.toConfig
      val checkType = outerConf.getString("type")
      val descr = Try {
        outerConf.getString("description")
      }.toOption
      val subtype   = outerConf.getString("subtype")
      val intConfig = outerConf.getObject("config").toConfig

      val params = getParams(intConfig)
      val metricListByCheck: List[(Check, String)] =
        checkType.toUpperCase match {
          case "SNAPSHOT" =>
            val metrics = intConfig.getStringList("metrics")
            val id = Try {
              outerConf.getString("id")
            }.toOption.getOrElse(subtype + ":" + checkType + ":" + metrics
              .mkString("+") + ":" + params.values.mkString(","))
            subtype match {
              // There also a way to use check name, but with additional comparsment rule
              case "BASIC_NUMERIC" =>
                val compRule = params("compRule").toString
                compRule.toUpperCase match {
                  case "GT" =>
                    if (params.contains("threshold"))
                      metrics.map { m =>
                        GreaterThanThresholdCheck(id,
                                                  descr.getOrElse(""),
                                                  Seq.empty[MetricResult],
                                                  params("threshold").toString.toDouble) -> m
                      }.toList
                    else if (params.contains("compareMetric"))
                      metrics.map { m =>
                        GreaterThanMetricCheck(id,
                                               descr.getOrElse(""),
                                               Seq.empty[MetricResult],
                                               params("compareMetric").toString) -> m
                      }.toList
                    else throw MissingParameterInException(subtype)
                  case "LT" =>
                    if (params.contains("threshold"))
                      metrics.map { m =>
                        LessThanThresholdCheck(id,
                                               descr.getOrElse(""),
                                               Seq.empty[MetricResult],
                                               params("threshold").toString.toDouble) -> m
                      }.toList
                    else if (params.contains("compareMetric"))
                      metrics.map { m =>
                        LessThanMetricCheck(id,
                                            descr.getOrElse(""),
                                            Seq.empty[MetricResult],
                                            params("compareMetric").toString) -> m
                      }.toList
                    else throw MissingParameterInException(subtype)
                  case "EQ" =>
                    if (params.contains("threshold"))
                      metrics.map { m =>
                        EqualToThresholdCheck(id,
                                              descr.getOrElse(""),
                                              Seq.empty[MetricResult],
                                              params("threshold").toString.toDouble) -> m
                      }.toList
                    else if (params.contains("compareMetric"))
                      metrics.map { m =>
                        EqualToMetricCheck(id,
                                           descr.getOrElse(""),
                                           Seq.empty[MetricResult],
                                           params("compareMetric").toString) -> m
                      }.toList
                    else throw MissingParameterInException(subtype)
                }
              case "GREATER_THAN" =>
                if (params.contains("threshold"))
                  metrics.map { m =>
                    GreaterThanThresholdCheck(id,
                                              descr.getOrElse(""),
                                              Seq.empty[MetricResult],
                                              params("threshold").toString.toDouble) -> m
                  }.toList
                else if (params.contains("compareMetric"))
                  metrics.map { m =>
                    GreaterThanMetricCheck(id,
                                           descr.getOrElse(""),
                                           Seq.empty[MetricResult],
                                           params("compareMetric").toString) -> m
                  }.toList
                else throw MissingParameterInException(subtype)
              case "LESS_THAN" =>
                if (params.contains("threshold"))
                  metrics.map { m =>
                    LessThanThresholdCheck(id,
                                           descr.getOrElse(""),
                                           Seq.empty[MetricResult],
                                           params("threshold").toString.toDouble) -> m
                  }.toList
                else if (params.contains("compareMetric"))
                  metrics.map { m =>
                    LessThanMetricCheck(id,
                                        descr.getOrElse(""),
                                        Seq.empty[MetricResult],
                                        params("compareMetric").toString) -> m
                  }.toList
                else throw MissingParameterInException(subtype)
              case "EQUAL_TO" =>
                if (params.contains("threshold"))
                  metrics.map { m =>
                    EqualToThresholdCheck(id,
                                          descr.getOrElse(""),
                                          Seq.empty[MetricResult],
                                          params("threshold").toString.toDouble) -> m
                  }.toList
                else if (params.contains("compareMetric"))
                  metrics.map { m =>
                    EqualToMetricCheck(id,
                                       descr.getOrElse(""),
                                       Seq.empty[MetricResult],
                                       params("compareMetric").toString) -> m
                  }.toList
                else throw MissingParameterInException(subtype)
              case "DIFFER_BY_LT" =>
                if (params.contains("threshold") && params.contains("compareMetric"))
                  metrics.map { m =>
                    DifferByLTMetricCheck(id,
                                          descr.getOrElse(""),
                                          Seq.empty[MetricResult],
                                          params("compareMetric").toString,
                                          params("threshold").toString.toDouble) -> m
                  }.toList
                else throw MissingParameterInException(subtype)
              case x => throw IllegalParameterException(x)
            }
          case "TREND" =>
            val metrics = intConfig.getStringList("metrics")
            val id = Try {
              outerConf.getString("id")
            }.toOption.getOrElse(subtype + ":" + checkType + ":" + metrics
              .mkString("+") + ":" + params.values.mkString(","))
            val rule      = intConfig.getString("rule")
            val startDate = Try { params("startDate").toString }.toOption
            subtype match {
              case "TOP_N_RANK_CHECK" =>
                if (params.contains("threshold") && params.contains("timewindow"))
                  metrics.flatMap { m =>
                    {
                      val basecheck =
                        TopNRankCheck(id,
                                      descr.getOrElse(""),
                                      Seq.empty[MetricResult],
                                      rule,
                                      params("threshold").toString.toDouble,
                                      params("timewindow").toString.toInt,
                                      startDate)
                      generateMetricSubId(m, params("targetNumber").toString.toInt)
                        .map(x => basecheck -> x)
                    }
                  }.toList
                else throw MissingParameterInException(subtype)
              case "AVERAGE_BOUND_FULL_CHECK" =>
                if (params.contains("threshold") && params.contains("timewindow"))
                  metrics.map { m =>
                    AverageBoundFullCheck(
                      id,
                      descr.getOrElse(""),
                      Seq.empty[MetricResult],
                      rule,
                      params("threshold").toString.toDouble,
                      params("timewindow").toString.toInt,
                      startDate
                    ) -> m
                  }.toList
                else throw MissingParameterInException(subtype)
              case "AVERAGE_BOUND_RANGE_CHECK" =>
                if (params.contains("thresholdUpper") && params.contains("thresholdLower") && params.contains(
                      "timewindow"))
                  metrics.map { m =>
                    AverageBoundRangeCheck(
                      id,
                      descr.getOrElse(""),
                      Seq.empty[MetricResult],
                      rule,
                      params("thresholdUpper").toString.toDouble,
                      params("thresholdLower").toString.toDouble,
                      params("timewindow").toString.toInt,
                      startDate
                    ) -> m
                  }.toList
                else throw MissingParameterInException(subtype)

              case "AVERAGE_BOUND_UPPER_CHECK" =>
                if (params.contains("threshold") && params.contains("timewindow"))
                  metrics.map { m =>
                    AverageBoundUpperCheck(
                      id,
                      descr.getOrElse(""),
                      Seq.empty[MetricResult],
                      rule,
                      params("threshold").toString.toDouble,
                      params("timewindow").toString.toInt,
                      startDate
                    ) -> m
                  }.toList
                else throw MissingParameterInException(subtype)
              case "AVERAGE_BOUND_LOWER_CHECK" =>
                if (params.contains("threshold") && params.contains("timewindow"))
                  metrics.map { m =>
                    AverageBoundLowerCheck(
                      id,
                      descr.getOrElse(""),
                      Seq.empty[MetricResult],
                      rule,
                      params("threshold").toString.toDouble,
                      params("timewindow").toString.toInt,
                      startDate
                    ) -> m
                  }.toList
                else throw MissingParameterInException(subtype)
              case x => throw IllegalParameterException(x)
            }
          case "SQL" => List.empty
          case x     => throw IllegalParameterException(x)
        }

      metricListByCheck
    }

    metricListByCheck
  }

  /**
    * Parses targets from configuration file
    * @return Map of (target_id, target_config)
    */
  private def getTargetsConfigMap: Map[String, List[TargetConfig]] = {

    val optionalTargetList = Try { configObj.getObjectList("Targets").toList }.toOption
    optionalTargetList match {
      case Some(targetList) =>
        val parsedList = targetList.map { trg =>
          val outerConf = trg.toConfig
          val inConfig   = outerConf.getObject("config").toConfig

          val tipo      = outerConf.getString("type")
          val name = Try(outerConf.getString("id")).getOrElse(tipo)

          val fileFormat = inConfig.getString("fileFormat")
          val path       = inConfig.getString("path")

          val delimiter = Try(inConfig.getString("delimiter")).toOption
          val quote     = Try(inConfig.getString("quote")).toOption
          val escape    = Try(inConfig.getString("escape")).toOption

          val quoteMode = Try(inConfig.getString("quoteMode")).toOption

          val date = Try(inConfig.getString("date")).toOption

          val hdfsTargetConfig = HdfsTargetConfig(name, fileFormat, path, delimiter, quote, escape, date, quoteMode)

          tipo.toUpperCase match {
            case "SYSTEM" =>
              val checkList: Seq[String] = outerConf.getStringList("checkList").toList
              val mailList: Seq[String] = outerConf.getStringList("mailingList").toList
              tipo -> SystemTargetConfig(name, checkList, mailList, hdfsTargetConfig)
            case _ =>
              tipo -> hdfsTargetConfig
          }
        }
        parsedList.groupBy(_._1).map { case (k, v) => (k, v.map(_._2)) }
      case None => Map.empty
    }
  }

  private def getLoadChecks: Map[String, Seq[LoadCheck]] = {
    val checkListConfOpt = Try { configObj.getObjectList("LoadChecks").toList }.toOption
    checkListConfOpt match {
      case Some(checkList) =>
        checkList
          .map(x => {
            val conf = x.toConfig

            val id: String     = conf.getString("id")
            val tipo: String   = conf.getString("type")
            val source: String = conf.getString("source")
            val result: AnyRef = conf.getAnyRef("option")

            if (LoadCheckEnum.contains(tipo)) {
              val check: LoadCheck = LoadCheckEnum
                .getCheckClass(tipo)
                .getConstructor(classOf[String], classOf[String], classOf[String], classOf[AnyRef])
                .newInstance(id, tipo, source, result)
                .asInstanceOf[LoadCheck]

              (source, check)
            } else throw new IllegalArgumentException(s"Unknown Load Check type: $tipo")

          })
          .groupBy(_._1)
          .map { case (k, v) => (k, v.map(_._2)) }
      case None => Map.empty[String, Seq[LoadCheck]]
    }

  }

  /**
    * Utilities
    */
  /**
    * Processes parameter sub-configuration
    * Made to prevent unexpected parameters and their values
    * @param ccf parameter configuration
    * @return mapped parameter
    */
  private def getParams(ccf: Config): Map[String, Any] = {
    Try { ccf.getConfig("params") }.toOption match {
      case Some(p) =>
        (for {
          entry: Entry[String, ConfigValue] <- p.entrySet()
          key = entry.getKey
          value = key match {
            case "threshold"      => p.getDouble(key)
            case "thresholdUpper" => p.getDouble(key)
            case "thresholdLower" => p.getDouble(key)
            case "timewindow"     => p.getInt(key)
            case "compareMetric"  => p.getString(key)
            case "compareValue"   => p.getString(key)
            case "targetValue"    => p.getString(key)
            case "maxCapacity"    => p.getInt(key)
            case "accuracyError"  => p.getDouble(key)
            case "targetNumber"   => p.getInt(key)
            case "targetSideNumber" =>
              p.getDouble(key) // move to irrelevant params
            case "domain"     => p.getStringList(key).toSet
            case "startDate"  => p.getString(key)
            case "compRule"   => p.getString(key)
            case "dateFormat" => p.getString(key)
            case "regex"      => p.getString(key)
            case x =>
              log.error(s"${key.toUpperCase} is an unexpected parameters from config!")
              throw IllegalParameterException(x)
          }
        } yield (key, value)).toMap
      case _ => Map.empty[String, Any]
    }
  }

  /**
    * Parses composed metrics from configuration file
    * @return List of composed metrics
    */
  private def getComposedMetrics: List[ComposedMetric] = {
    val metricsList: List[ConfigObject] =
      Try(configObj.getObjectList("ComposedMetrics").toList)
        .getOrElse(List.empty[ConfigObject])

    val metricFileList: List[ComposedMetric] = metricsList.map { mts =>
      val outerConf = mts.toConfig
      val id        = outerConf.getString("id")
      val name      = outerConf.getString("name")
      val descr     = outerConf.getString("description")
      val formula   = outerConf.getString("formula")

      ComposedMetric(
        id,
        name,
        descr,
        formula,
        Map.empty
      )
    }

    metricFileList
  }

  /**
    * Processes schema (exact/path)
    * @param conf configuration
    * @return Option(Path/List of fields)
    */
  private def getStructSchema(conf: Config): Option[Any] = {
    import scala.collection.JavaConverters._

    //check that is present in schema parameter: path or exact schema
    val list = Try {
      conf.getObjectList("schema")
    }.toOption match {
      case Some(p) => p.asScala
      case _ =>
        Try {
          conf.getString("schema")
        }.toOption match {
          case Some(s) => return Some(s)
          case _       => mutable.Buffer.empty[ConfigObject]
        }
    }

    // exact schema parsing
    val ll: Seq[StructColumn] = list.toList.map { x =>
      val cf = x.toConfig
      StructColumn(
        cf.getString("name"),
        cf.getString("type"),
        if (cf.getString("type") == "date") Option(cf.getString("format"))
        else None
      )
    }
    if (ll.isEmpty) None else Some(ll)

  }

  /**
    * Processes fixed schema (exact/path)
    * @param conf configuration
    * @return Option(Path/List of fields)
    */
  private def getFixedStructSchema(conf: Config): Option[Any] = {

    val list = Try {
      conf.getObjectList("schema")
    }.toOption match {
      case Some(p) => p.toList
      case _ =>
        Try {
          conf.getString("schema")
        }.toOption match {
          case Some(s) => return Some(s)
          case _       => mutable.Buffer.empty[ConfigObject]
        }
    }

    val ll: Seq[StructFixedColumn] = list.toList.map { x =>
      val cf = x.toConfig
      StructFixedColumn(
        cf.getString("name"),
        cf.getString("type"),
        cf.getInt("length"),
        if (cf.getString("type") == "date") Option(cf.getString("format"))
        else None
      )
    }

    if (ll.isEmpty) None else Some(ll)

  }

  /**
    * Parses fieldLength way to provide the schema
    * @param inputConf input configuration
    * @return Option list of Fixed columns
    */
  private def getFixedSchema(inputConf: Config): Option[List[StructFixedColumn]] = {
    Try(inputConf.getStringList("fieldLengths").toList.map { str =>
      val splitted: Array[String] = str.split(":")
      StructFixedColumn(splitted(0), "string", splitted(1).toInt)
    }).toOption
  }

  private def getPostprocessorConfig: Seq[BasicPostprocessor] = {
    val ppConfs: Seq[ConfigObject] = Try(configObj.getObjectList("Postprocessing").toList).getOrElse(List.empty)
    log.info(s"Found ${ppConfs.size} configurations.")
    ppConfs.map(ppc => {
      val conf = ppc.toConfig
      val mode = conf.getString("mode")
      Try(PostprocessorType.withName(mode.toLowerCase)).toOption match {
        case Some(meta) =>
          log.info(s"Found configuration with mode : $mode")
          val inner: Config = conf.getConfig("config")

          meta.service.getConstructors.head
            .newInstance(inner)
            .asInstanceOf[BasicPostprocessor]
        case None =>
          log.warn("Wrong mode name!")
          throw IllegalParameterException(mode)
      }
    })
  }

}
