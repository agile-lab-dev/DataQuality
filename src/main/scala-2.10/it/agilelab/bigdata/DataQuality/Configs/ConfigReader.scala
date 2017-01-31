package it.agilelab.bigdata.DataQuality.configs

import java.io.File
import java.util.Map.Entry

import it.agilelab.bigdata.DataQuality.checks.Check
import it.agilelab.bigdata.DataQuality.checks.SnapshotChecks._
import it.agilelab.bigdata.DataQuality.exceptions.{MissingParameterInException, IllegalParameterException}
import it.agilelab.bigdata.DataQuality.metrics.SourceProcessor.{MetricId, FileId}
import it.agilelab.bigdata.DataQuality.metrics._
import it.agilelab.bigdata.DataQuality.sources.{ HdfsFile, SourceConfig}
import it.agilelab.bigdata.DataQuality.targets.{HdfsTargetConfig, TargetConfig}
import it.agilelab.bigdata.DataQuality.utils.Logging
import com.typesafe.config.{Config, ConfigFactory, ConfigObject, ConfigValue}

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.util.Try

class ConfigReader (configNameFile: String) extends  Logging {

  /**
    * Parsed config OBJECTS
    *
    */

  //load conf file
  val configObj: Config = ConfigFactory.parseFile(new File(configNameFile)).resolve()

  //parse sources, metrics, checks, it.agilelab.bigdata.targets
  val sourcesConfigMap: Map[FileId, SourceConfig] = getSourcesById

  val metricsBySourceList: List[(FileId, Metric)] = getMetricsBySource
  lazy val metricsBySourceMap: Map[FileId, List[Metric]] = metricsBySourceList.groupBy(_._1).mapValues(_.map(_._2))

  val metricsByChecksList: List[(Check, MetricId)] = getMetricByCheck
  lazy val metricsByCheckMap: Map[Check, List[MetricId]] = metricsByChecksList.groupBy(_._1).mapValues(_.map(_._2))

  lazy val composedMetrics = getComposedMetrics

  lazy val targetsConfigMap = getTargetsConfigMap





  /**
    * Config PARSER
    *  - it.agilelab.bigdata.Sources
    *  - it.agilelab.bigdata.Metrics
    *  - it.agilelab.bigdata.Checks
    *  - ComposedMetrics
    *  - Targets
    *
    */

  private def getSourcesById: Map[String, SourceConfig] = {
    val sourcesList: List[ConfigObject] = configObj.getObjectList("it/agilelab/bigdata/Sources").toList

    sourcesList.map{
      src =>
         val generalConfig = src.toConfig
         generalConfig.getString("type") match {
          case "HDFS"   => {
            val id        = generalConfig.getString("id")
            val path      = generalConfig.getString("path")
            val fileType  = generalConfig.getString("fileType")
            val separator = Try(generalConfig.getString("separator")).toOption
            val header    = Try(generalConfig.getBoolean("header")).getOrElse(false)
            val date      = Try(generalConfig.getString("date")).getOrElse("")
            val deps      = Try(generalConfig.getStringList("deps").toList).getOrElse(List.empty[FileId])
            val schema    = generalConfig.getString("fileType") match {
              case "fixed"   => {
                if      (Try(generalConfig.getObjectList("schema")).isSuccess) getFixedStructSchema(generalConfig)
                else if (Try(generalConfig.getStringList("fieldLengths")).isSuccess) getFixedSchema(generalConfig)
                else {
                  val allKeys = generalConfig.entrySet().map(_.getKey)
                  throw new IllegalParameterException("\n CONFIG: "+allKeys.mkString(" - "))
                }
              }
              case x => throw new IllegalParameterException(x)
            }

            id -> HdfsFile(id, path, fileType, separator, header, date, deps, schema)
          }
          case x => throw new IllegalParameterException(x)
        }
    }.toMap
  }


  private def getMetricsBySource: List[(FileId, Metric)] = {
    val metricsList: List[ConfigObject] = configObj.getObjectList("it/agilelab/bigdata/Metrics").toList

    val metricFileList: List[(String, Metric)] = metricsList.map {
      mts =>
        val outerConf = mts.toConfig
        val metricType = outerConf.getString("type")
        val id = outerConf.getString("id")
        val name = outerConf.getString("name")
        val descr = outerConf.getString("description")
        val intConfig = outerConf.getObject("config").toConfig
        val params = getParams(intConfig)
        val applyFile = intConfig.getString("file")

        metricType match {
          case "COLUMN" => {
            val applyColumn = intConfig.getString("column")
            applyFile -> ColumnMetric(id, name , descr, applyFile, applyColumn, params)
          }
          case "FILE" => {
            applyFile -> FileMetric(id, name, descr, applyFile, params)
          }
          case x => throw new IllegalParameterException(x)
        }
    }

    metricFileList
  }


  private def getMetricByCheck:  List[(Check, MetricId)] = {
    val checksList: List[ConfigObject] = configObj.getObjectList("it/agilelab/bigdata/Checks").toList

    val metricListByCheck = checksList.flatMap {
      chks =>

        val outerConf = chks.toConfig
        val checkType = outerConf.getString("type")
        val descr     = Try{outerConf.getString("description")}.toOption
        val name      = outerConf.getString("name")
        val subtype   = outerConf.getString("subtype")
        val intConfig = outerConf.getObject("config").toConfig
        val metrics   = intConfig.getStringList("metrics")
        val params    = getParams(intConfig)

        val metricListByCheck: List[(Check, MetricId)] = checkType match {
          case "snapshot" => {

            subtype match {
              case "GREATER_THAN"      => {
                if(params.contains("threshold"))
                  metrics.map { m => GreaterThanThresholdCheck("Check_" + metrics.mkString("+"), name, descr.getOrElse(""), Seq.empty[MetricResult], params("threshold").toString.toDouble) -> m }.toList
                else if (params.contains("compareMetric"))
                  metrics.map { m => GreaterThanMetricCheck("Check_" + metrics.mkString("+"), name, descr.getOrElse(""),  Seq.empty[MetricResult], params("compareMetric").toString ) -> m }.toList
                else throw new MissingParameterInException(subtype)
              }
              case "LESS_THAN"         => {
                if (params.contains("threshold"))
                  metrics.map { m => LessThanThresholdCheck("Check_" + metrics.mkString("+"), name, descr.getOrElse(""), Seq.empty[MetricResult], params("threshold").toString.toDouble) -> m }.toList
                else if (params.contains("compareMetric"))
                  metrics.map { m => LessThanMetricCheck("Check_" + metrics.mkString("+"), name, descr.getOrElse(""),  Seq.empty[MetricResult], params("compareMetric").toString ) -> m }.toList
                else throw new MissingParameterInException(subtype)
              }
              case "EQUAL_TO"          => {
                if(params.contains("threshold"))
                  metrics.map { m => EqualToThresholdCheck("Check_" + metrics.mkString("+"), name, descr.getOrElse(""), Seq.empty[MetricResult], params("threshold").toString.toDouble) -> m }.toList
                else if (params.contains("compareMetric"))
                  metrics.map { m => EqualToMetricCheck("Check_" + metrics.mkString("+"), name, descr.getOrElse(""),  Seq.empty[MetricResult], params("compareMetric").toString ) -> m }.toList
                else throw new MissingParameterInException(subtype)
              }
              case x => throw new IllegalParameterException(x)
            }
          }
          case x => throw new IllegalParameterException(x)
        }

        metricListByCheck
    }

    metricListByCheck
  }


  private def getTargetsConfigMap: Map[String, TargetConfig] = {
    val targetList: List[ConfigObject] = configObj.getObjectList("Targets").toList
    targetList.map{
      trg =>
        val outerConf = trg.toConfig
        val ruleName  = outerConf.getString("ruleName")
        val tipo      = outerConf.getString("type")
        val fileFormat = outerConf.getString("fileFormat")

        ruleName match {
          case _  => {

            val inConfig = outerConf.getObject("config").toConfig
            val path       = inConfig.getString("path")
            val delimiter  = Try{inConfig.getString("delimiter")}.toOption
            val name       = Try{inConfig.getString("name")}.getOrElse(tipo)
            val savemode   = Try{inConfig.getString("savemode")}.toOption
            val deps       = Try{inConfig.getStringList("deps").toList}.getOrElse(List.empty[String])

            tipo -> HdfsTargetConfig(name, fileFormat, path, ruleName, delimiter, "", deps, savemode)
          }
        }
    }.toMap
  }






  /**
    * Utilities
    */
  private def getParams(ccf: Config): Map[String, Any] = {
    import scala.collection.JavaConverters._

    val list = Try{ccf.getObjectList("params")}.toOption match {
      case Some(p) => p.asScala
      case _       => mutable.Buffer.empty[ConfigObject]
    }

    list.nonEmpty match {
      case true => {
        val mm: Map[String, Any] = (for {
          item: ConfigObject <- list
          entry: Entry[String, ConfigValue] <- item.entrySet().asScala
          key = entry.getKey
          value = key match {
            case "threshold" => entry.getValue.unwrapped().toString.toInt
            case "compareMetric" =>  entry.getValue.unwrapped().toString
            case "domainSet" => entry.getValue.unwrapped().toString.split(":").toSet
            case x => {
              println(s"${key.toUpperCase} is an unespected parameters from config!")
              throw new IllegalParameterException(x)
            }
          }
        } yield (key, value)).toMap
        mm
      }
      case false => Map.empty[String, Any]
    }
  }

  private def getComposedMetrics: List[ComposedMetric] = {
    val metricsList: List[ConfigObject] = Try(configObj.getObjectList("ComposedMetrics").toList).getOrElse(List.empty[ConfigObject])

    val metricFileList: List[ComposedMetric] = metricsList.map {
      mts =>
        val outerConf = mts.toConfig
        val id = outerConf.getString("id")
        val name = outerConf.getString("name")
        val descr = outerConf.getString("description")
        val formula = outerConf.getString("formula")

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

  private def getStructSchema(conf:Config): Option[List[StructField]] = {
    import scala.collection.JavaConverters._

    val list = Try{conf.getObjectList("schema")}.toOption match {
      case Some(p) => p.asScala
      case _       => mutable.Buffer.empty[ConfigObject]
    }
    list.nonEmpty match {
      case true => {

        val ll = list.toList.map{ x =>
          val cf = x.toConfig
          StructField(
            cf.getString("name"),
            cf.getString("type"),
            if(cf.getString("type")=="date") Option(cf.getString("format")) else None
          )
        }
        Option(ll)
      }

      case false => None
    }
  }

  private def getFixedStructSchema(conf:Config): Option[List[StructFixedField]] = {

    val list= Try{conf.getObjectList("schema")}.toOption match {
      case Some(p) => p.toList
      case _       => List.empty[ConfigObject]
    }
    list.nonEmpty match {
      case true => {

        val ll = list.map{ x =>
          val cf = x.toConfig
          StructFixedField(
            cf.getString("name"),
            cf.getString("type"),
            cf.getInt("length"),
            if(cf.getString("type")=="date") Option(cf.getString("format")) else None
          )
        }
        Option(ll)
      }

      case false => None
    }
  }

  private def getFixedSchema(inputConf:Config): Option[List[StructFixedField]] = {
    Try(inputConf.getStringList("fieldLengths").toList.map{ str =>
      val splitted: Array[FileId] = str.split(":")
      StructFixedField(splitted(0), "string", splitted(1).toInt)
    }).toOption
  }



}


