package dbmodel.config

import com.typesafe.config._
import dbmodel.checks.{Check, SnapshotCheck, SqlCheck, TrendCheck}
import dbmodel.config.ParamParser.parseParam
import dbmodel.metrics.{ColumnMetric, ComposedMetric, FileMetric, Metric}
import dbmodel.sources.{Source, _}
import dbmodel.targets.Target
import dbmodel.targets.Target.TargetType
import dbmodel.ModelUtils._
import dbmodel.sources.Source.SourceType
import org.squeryl.Query
import dbmodel.MyOwnTypeMode._
import scala.collection.JavaConversions._

/**
  * Created by Egor Makhov on 04/09/2017.
  */
object ConfigWriter {

  def addFilterVirtualSources(sources: Option[List[String]]): Option[List[String]] ={
    sources match {
      case Some(s: List[String]) =>Some(s ++ Source.  getVirtualFilterBySourceIds(s).toList)
      case _ => None
    }


  }

  def generateConfig(psources: Option[List[String]] = None, virtualFilter:Boolean = false): Config = {

    val sources = if (virtualFilter) addFilterVirtualSources(psources)  else psources

    lazy val dbConfList = generateDBConfList(sources)
    lazy val sourceConfList = generateSourceConfList(sources)

    lazy val virtualSourceConfList = generateVirtualSourcesConfList(sources)
    lazy val metricConfList = generateMetricConfList(sources)
    lazy val checkConfList = generateCheckConfList(sources)
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

  private def generateDBConfList(sources:Option[List[String]]): Seq[ConfigObject] = {

    val databases = if (sources.isEmpty) Database.getAll() else Database.getBySources(sources.get)
      databases.iterator.foldLeft(List.empty[ConfigObject])((list, db) => {
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

  private def generateSourceConfList(sourceIDs:Option[List[String]]=None): Seq[ConfigObject] = {

    val sources = if (sourceIDs.isEmpty) Source.getAll()  else Source.getBySourceIDs(sourceIDs.get)

    sources.iterator.foldLeft(List.empty[ConfigObject])((list, src) => {
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

  private def generateVirtualSourcesConfList(sourceIDs:Option[List[String]]=None): Seq[ConfigObject] = {

    val virtuals = if (sourceIDs.isEmpty) Source.getAll(filter = Some(SourceType.virtual)) else Source.getBySourceIDs(sourceIDs.get,filter = Some(SourceType.virtual))
    virtuals.iterator.foldLeft(List.empty[ConfigObject])(
      (list, src) => {
        val vs = VirtualSourceDB.getDetailed(src.id)._2

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

  private def generateMetricConfList(sourceIDs:Option[List[String]]=None): (List[ConfigObject], List[ConfigObject]) = {
    //  Metrics
    val metrics = if (sourceIDs.isEmpty) Metric.getAll() else Metric.getMetricsBySourceIds(sourceIDs.get)
    metrics.iterator.foldLeft((List.empty[ConfigObject], List.empty[ConfigObject]))(
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

  private def generateCheckConfList(sourceIDs:Option[List[String]]=None): Seq[ConfigObject] = {
    val metricIds= if (sourceIDs.isEmpty) None else Some( Metric.getMetricsBySourceIds(sourceIDs.get).toList.map(m=>m.id))
    val checks = if (metricIds.isEmpty) Check.getAll() else Check.getChecksByMetricIDs(metricIds.get)
    checks.iterator.foldLeft(List.empty[ConfigObject])(
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
