package models.config

import com.typesafe.config._
import models.checks.{Check, SnapshotCheck, SqlCheck, TrendCheck}
import models.config.ParamParser.parseParam
import models.metrics.{ColumnMetric, ComposedMetric, FileMetric, Metric}
import models.sources.{Source, _}
import models.targets.Target
import models.targets.Target.TargetType
import models.ModelUtils._
import models.sources.Source.SourceType

import scala.collection.JavaConversions._

/**
  * Created by Egor Makhov on 04/09/2017.
  */
object ConfigWriter {

  def generateConfig: Config = {

    lazy val dbConfList = generateDBConfList()
    lazy val sourceConfList = generateSourceConfList()
    lazy val virtualSourceConfList = generateVirualSourcesConfList()
    lazy val metricConfList = generateMetricConfList()
    lazy val checkConfList = generateCheckConfList()
    lazy val targetConfList = generateTargetConfList()

    ConfigFactory.empty()
      .withValue("Databases", ConfigValueFactory.fromIterable(dbConfList))
      .withValue("Sources", ConfigValueFactory.fromIterable(sourceConfList))
      .withValue("VirtualSources", ConfigValueFactory.fromIterable(virtualSourceConfList))
      .withValue("Metrics", ConfigValueFactory.fromIterable(metricConfList._1))
      .withValue("ComposedMetrics", ConfigValueFactory.fromIterable(metricConfList._2))
      .withValue("Checks", ConfigValueFactory.fromIterable(checkConfList))
      .withValue("Targets", ConfigValueFactory.fromIterable(targetConfList))
  }

  private def generateDBConfList(): Seq[ConfigObject] = {
    Database.getAll().iterator.foldLeft(List.empty[ConfigObject])((list, db) => {
      val conf = ConfigFactory.empty()
        .withValue("id", toConfigValue(db.id))
        .withValue("subtype", toConfigValue(db.subtype))

      val confMap: Map[String, Any] = Map(
        "host"->Some(db.host),
        "port"->db.port,
        "service"->db.service,
        "user"->db.user,
        "password"->db.password
      ).filter(x => x._2.isDefined).map(x => x._1 -> x._2.get)

      list.::(conf.withValue("config", ConfigValueFactory.fromMap(confMap)).root())
    })
  }

  private def generateSourceConfList(): Seq[ConfigObject] = {
    Source.getAll().iterator.foldLeft(List.empty[ConfigObject])((list, src) => {
      src.scType match {
        case "HDFS" =>
          val (source,curr):(Source, HdfsFile) = HdfsFile.getDetailed(src.id)

          val baseConfMap: Map[String, Any] = Map(
            "id" -> Some(curr.id),
            "type" -> Some(source.scType),
            "path" -> Some(curr.path),
            "fileType" -> Some(curr.fileType),
            "header" -> curr.header,
            "date" -> curr.date,
            "separator" -> curr.separator,
            "schema" -> curr.schemaPath
          ).filter(x => x._2.isDefined).map(x => x._1 -> x._2.get)

          val schemaConf = curr.schema.foldLeft(List.empty[ConfigObject])((list, fld) => {
            val fConf = ConfigFactory.empty()
              .withValue("name", toConfigValue(fld.fieldName))
              .withValue("type", toConfigValue(fld.fieldType))

            list.::(fConf.root())
          })

          val conf = if (schemaConf.nonEmpty)
            ConfigFactory.parseMap(baseConfMap)
              .withValue("keyFields", ConfigValueFactory.fromIterable(parseSeparatedString(source.keyFields)))
              .withValue("schema", ConfigValueFactory.fromIterable(schemaConf))
          else
            ConfigFactory.parseMap(baseConfMap)
              .withValue("keyFields", ConfigValueFactory.fromIterable(parseSeparatedString(source.keyFields)))

          list.::(conf.root())

        case "TABLE" =>
          val (source, curr): (Source,DBTable) = DBTable.getDetailed(src.id)
          val baseConfMap: Map[String, Any] = Map(
            "id" -> Some(curr.id),
            "type" -> Some("TABLE"),
            "database" -> Some(curr.database),
            "table" -> Some(curr.table),
            "username" -> curr.username,
            "password" -> curr.password
          ).filter(x => x._2.isDefined).map(x => x._1 -> x._2.get)

          val conf = ConfigFactory.parseMap(baseConfMap)
            .withValue("keyFields", ConfigValueFactory.fromIterable(parseSeparatedString(source.keyFields)))

          list.::(conf.root())

        case "HIVE" =>
          val (source, curr): (Source, HiveTable) = HiveTable.getDetailed(src.id)
          val baseConfMap: Map[String, Any] = Map(
            "id" -> curr.id,
            "type" -> "HIVE",
            "query" -> curr.query,
            // todo: Check date status in the core
            "date" -> curr.date
          )

          val conf = ConfigFactory.parseMap(baseConfMap)
            .withValue("keyFields", ConfigValueFactory.fromIterable(parseSeparatedString(source.keyFields)))

          list.::(conf.root())

        case _ => list

      }
    })
  }

  private def generateVirualSourcesConfList(): Seq[ConfigObject] = {
    Source.getAll(filter = Some(SourceType.virtual)).iterator.foldLeft(List.empty[ConfigObject])(
      (list, src) => {
        val vs = VirtualSource.getDetailed(src.id)._2

        val baseConfMap: Map[String, Any] = Map(
          "id" -> vs.id,
          "type" -> vs.tipo,
          "sql" -> vs.query
        )
        val parentSources = List(Some(vs.left),vs.right).flatten

        val conf = ConfigFactory.parseMap(baseConfMap)
          .withValue("parentSources", ConfigValueFactory.fromIterable(parentSources))
          .withValue("keyFields", ConfigValueFactory.fromIterable(parseSeparatedString(src.keyFields)))

        list.::(conf.root())
      }
    )
  }

  private def generateMetricConfList(): (List[ConfigObject], List[ConfigObject]) = {
    //  Metrics
    Metric.getAll().iterator.foldLeft((List.empty[ConfigObject], List.empty[ConfigObject]))(
      (listTpl, met) => {
        met.mType match {
          case "FILE" =>
            val curr: (Metric, FileMetric) = FileMetric.getById(met.id)
            val baseConf: Config = ConfigFactory.parseMap(
              Map(
                "id" -> curr._1.id,
                "type" -> "FILE",
                "name" -> curr._1.name,
                "description" -> curr._1.description
              ))
            val settingsConf: ConfigObject = ConfigFactory.parseMap(Map("file" -> curr._2.source)).root()

            (listTpl._1.::(baseConf.withValue("config", settingsConf).root()), listTpl._2)

          case "COLUMN" =>
            val curr: (Metric, ColumnMetric) = ColumnMetric.getById(met.id)
            val baseConf: Config = ConfigFactory.parseMap(
              Map(
                "id" -> curr._1.id,
                "type" -> "COLUMN",
                "name" -> curr._1.name,
                "description" -> curr._1.description
              ))

            val paramMap: Map[String, ConfigValue] = curr._2.parameters.map(mp => mp.name -> parseParam(mp.value)).toMap

            val settingsConf: ConfigObject = ConfigFactory.parseMap(Map(
              "file" -> curr._2.source
            )).withValue("params", ConfigFactory.parseMap(paramMap).root())
              .withValue("columns", ConfigValueFactory.fromIterable(parseSeparatedString(Some(curr._2.columns))))
              .root()

            (listTpl._1.::(baseConf.withValue("config", settingsConf).root()), listTpl._2)

          case "COMPOSED" =>
            val curr: (Metric, ComposedMetric) = ComposedMetric.getById(met.id)
            val baseConfMap: Map[String, Any] = Map(
              "id" -> curr._1.id,
              "name" -> curr._1.name,
              "description" -> curr._1.description,
              "formula" -> curr._2.formula
            )
            (listTpl._1, listTpl._2.::(ConfigFactory.parseMap(baseConfMap).root()))
        }
      })
  }

  private def generateCheckConfList(): Seq[ConfigObject] = {
    Check.getAll().iterator.foldLeft(List.empty[ConfigObject])(
      (list, chk) => {

        val baseConfMap: Map[String, String] = Map(
          "id" -> Some(chk.id),
          "type" -> Some(chk.cType),
          "subtype" -> Some(chk.subtype),
          "description" -> chk.description
        ).filter(x => x._2.isDefined).map(x => x._1 -> x._2.get)

        chk.cType.toUpperCase match {
          case "SQL" =>
            val curr: SqlCheck = SqlCheck.getById(chk.id)._2

            val settingMap: Map[String, String] = Map(
              "source" -> curr.database,
              "query" -> curr.query
            )
            val confObj: ConfigObject = ConfigFactory.parseMap(baseConfMap)
              .withValue("config", ConfigFactory.parseMap(settingMap).root())
              .root()

            list.::(confObj)

          case "SNAPSHOT" =>

            // todo: refactor check params
            val curr: SnapshotCheck = SnapshotCheck.getById(chk.id).single._2

            val paramMap = curr.parameters.map(mp => mp.name -> mp.value).toMap
            val metricList: Seq[String] = paramMap.get("compareMetric") match {
              case Some(m) => List(m, curr.metric)
              case None => List(curr.metric)
            }
            val confObj = ConfigFactory.empty()
              .withValue("metrics", ConfigValueFactory.fromIterable(metricList))
              .withValue("params", ConfigFactory.parseMap(paramMap).root())
              .root()
            val finalConf = ConfigFactory.parseMap(baseConfMap)
              .withValue("config", confObj)

            list.::(finalConf.root())

          case "TREND" =>
            val curr: TrendCheck = TrendCheck.getById(chk.id)._2

            val paramMap = curr.parameters.map(mp => mp.name -> parseParam(mp.value)).toMap
            val metricList: Seq[Object] = paramMap.get("compareMetric") match {
              case Some(m) => List(m, curr.metric)
              case None => List(curr.metric)
            }
            val confObj = ConfigFactory.empty()
              .withValue("rule", toConfigValue(curr.rule))
              .withValue("metrics", ConfigValueFactory.fromIterable(metricList))
              .withValue("params", ConfigFactory.parseMap(paramMap).root())
              .root()
            val finalConf = ConfigFactory.parseMap(baseConfMap)
              .withValue("config", confObj)

            list.::(finalConf.root())
        }
      })
  }

  private def generateTargetConfList(): Seq[ConfigObject] = {
    val allTargets = Target.getAll().iterator

    val sysTarConfigs: Seq[ConfigObject] = allTargets.filter(tar => tar.targetType == TargetType.system.toString).foldLeft(List.empty[ConfigObject])(
      (list, curr) => {
//        val curr: Target = Target.getDetailed(tar("id")).head
        val settingsMap: Map[String, Any] = Map(
          "fileFormat" -> Some(curr.fileFormat),
          "path" -> Some(curr.path),
          "delimiter" -> curr.delimiter,
          "savemode" -> curr.savemode,
          "partitions" -> curr.partitions
        ).filter(x => x._2.isDefined).map(x => x._1 -> x._2.get)
        val checks: Seq[String] = curr.checks.map(c => c.checkId).toList
        val mails: Seq[String] = curr.mails.map(m => m.address).toList
        val conf = ConfigFactory.empty()
          .withValue("id", toConfigValue(curr.id))
          .withValue("type", toConfigValue(curr.targetType))
          .withValue("checkList", ConfigValueFactory.fromIterable(checks))
          .withValue("mailingList", ConfigValueFactory.fromIterable(mails))
          .withValue("config", ConfigFactory.parseMap(settingsMap).root())

        list.::(conf.root())
      })

    val basicTarConfigs: Seq[ConfigObject] = allTargets.filter(tar => tar.targetType != TargetType.system.toString).foldLeft(List.empty[ConfigObject])(
      (list, curr) => {
//        val curr: Target = Target.getDetailed(tar("id")).head
        val settingsMap: Map[String, Any] = Map(
          "fileFormat" -> Some(curr.fileFormat),
          "path" -> Some(curr.path),
          "delimiter" -> curr.delimiter,
          "savemode" -> curr.savemode,
          "partitions" -> curr.partitions
        ).filter(x => x._2.isDefined).map(x => x._1 -> x._2.get)

        val conf = ConfigFactory.empty()
          .withValue("id", toConfigValue(curr.id))
          .withValue("type", toConfigValue(curr.targetType))
          .withValue("config", ConfigFactory.parseMap(settingsMap).root())

        list.::(conf.root())
      })

    sysTarConfigs ++ basicTarConfigs
  }

  // config value shortcut
  private def toConfigValue(value: Any): ConfigValue = {
    ConfigValueFactory.fromAnyRef(value)
  }

}
