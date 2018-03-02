package models.config

import java.io.File
import java.util

import com.typesafe.config.{Config, ConfigFactory, ConfigObject}
import models.checks._
import models.metrics._
import models.sources._
import models.ModelUtils._
import models.sources.Source.SourceType
import models.targets.{Mail, Target, TargetToChecks}
import org.squeryl.PrimitiveTypeMode.inTransaction

import scala.collection.JavaConversions._
import scala.util.Try

/**
  * Created by Egor Makhov on 07/08/2017.
  */
object ConfigReader {

  /**
    * Parsed config OBJECTS
    *
    */
  def parseConfiguration(configFile: File): Unit = {
    val configObj: Config = ConfigFactory.parseFile(configFile).resolve()

    // ORDER MATTERS!
    parseDatabases(configObj)
    parseSources(configObj)
    parseVirtualSources(configObj)
    parseMetrics(configObj)
    parseComposedMetrics(configObj)
    parseChecks(configObj)
    parseTargets(configObj)
  }

  private def parseDatabases(configObj: Config): Unit = {
    try {
      val databaseList = configObj.getObjectList("Databases").toList
      inTransaction(
        databaseList.foreach { db =>

          val generalConfig = db.toConfig
          val outerConfig = generalConfig.getConfig("config")

          val id = generalConfig.getString("id")
          val subtype = generalConfig.getString("subtype")
          val host = outerConfig.getString("host")

          val port: Option[Int] = Try(outerConfig.getString("port").toInt).toOption
          val service: Option[String] = Try(outerConfig.getString("service")).toOption
          val user: Option[String] = Try(outerConfig.getString("user")).toOption
          val password: Option[String] = Try(outerConfig.getString("password")).toOption

          new Database(id, subtype, host, port, service, user, password).insert()
        }
      )
    } catch {
      case e: Exception => println(e.toString)
    }
  }

  private def parseSources(configObj: Config): Unit = {
    val sourceList = configObj.getObjectList("Sources").toList
    inTransaction(
      sourceList.foreach { sc =>
        val generalConfig = sc.toConfig
        val tipo = generalConfig.getString("type")

        val keyFields: Option[Seq[String]] = Try(generalConfig.getStringList("keyFields").toSeq).toOption
        val kfAsString = toSeparatedString(keyFields.getOrElse(Seq.empty))

        val id = generalConfig.getString("id")
        new Source(id, tipo, kfAsString).insert()

        tipo match {
          case "HDFS" =>
            val path = generalConfig.getString("path")
            val fileType = generalConfig.getString("fileType")
            val separator = Try(generalConfig.getString("separator")).toOption
            val header: Option[Boolean] = Try(generalConfig.getBoolean("header")).toOption
            val date = Try(generalConfig.getString("date")).toOption

            Try {generalConfig.getObjectList("schema")}.toOption match {
              case Some(p) =>
                new HdfsFile(id, path, fileType, separator, header, None, date).insert()

                p.foreach(field => {
                  val conf = field.toConfig
                  val name = conf.getString("name")
                  val tipo = conf.getString("type")

                  new FileField(id, name, tipo).insert()
                })
              case _ =>
                val schemaStr: Option[String] = Try {generalConfig.getString("schema")}.toOption
                new HdfsFile(id, path, fileType, separator, header, schemaStr, date).insert()
            }
          case "TABLE" =>
            val database = generalConfig.getString("database")
            val table = generalConfig.getString("table")
            val username = Try{generalConfig.getString("username")}.toOption
            val password = Try{generalConfig.getString("password")}.toOption

            new DBTable(id, database, table, username, password).insert()
          case "HIVE" =>
            val date = generalConfig.getString("date")
            val query = generalConfig.getString("query")

            new HiveTable(id, date, query).insert()
        }
      }
    )
  }

  private def parseVirtualSources(configObj: Config): Unit = {
    val sourceList = configObj.getObjectList("VirtualSources").toList
    inTransaction( sourceList.foreach { sc =>
      val generalConfig = sc.toConfig

      val keyFields: Option[Seq[String]] = Try(generalConfig.getStringList("keyFields").toSeq).toOption
      val kfAsString = toSeparatedString(keyFields.getOrElse(Seq.empty))

      val id = generalConfig.getString("id")
      new Source(id, SourceType.virtual.toString, kfAsString).insert()

      val tipo = generalConfig.getString("type")
      val query = generalConfig.getString("sql")
      val (left: String, right: Option[String]) = {
        val list = generalConfig.getStringList("parentSources")
        (list.head, Try(list(1)).toOption)
      }

      new VirtualSource(id, tipo, left, right, query).insert()
    })
  }

  private def parseMetrics(configObj: Config): Unit = {
    val metricList = configObj.getObjectList("Metrics").toList
    inTransaction(
      metricList.foreach { met =>

        val generalConfig = met.toConfig
        val outerConfig = generalConfig.getConfig("config")

        val id = generalConfig.getString("id")
        val name = generalConfig.getString("name")
        val tipo = generalConfig.getString("type")
        val description = generalConfig.getString("description")

        tipo match {
          case "FILE" =>
            val source = outerConfig.getString("file")

            Metric(id, name, tipo, description).insert()
            FileMetric(id, source).insert()
          case "COLUMN" =>
            val source = outerConfig.getString("file")
            val column = toSeparatedString(outerConfig.getStringList("columns")).get

            Metric(id, name, tipo, description).insert()
            ColumnMetric(id, source, column).insert()
        }

        Try(outerConfig.getObject("params").foreach(param => {
          MetricParameter(id, param._1, param._2.unwrapped.toString).insert()
        }))
      }
    )
  }

  private def parseComposedMetrics(configObj: Config): Unit = {
    val metricList = configObj.getObjectList("ComposedMetrics").toList
    inTransaction(
      metricList.foreach { met =>
        val generalConfig = met.toConfig

        val id = generalConfig.getString("id")
        val name = generalConfig.getString("name")
        val description = generalConfig.getString("description")

        // TODO: validate function
        val formula = generalConfig.getString("formula")

        Metric(id, name, "COMPOSED", description).insert()
        ComposedMetric(id, formula).insert()
      }
    )
  }

  private def parseChecks(configObj: Config): Unit = {
    val checkList = configObj.getObjectList("Checks").toList
    inTransaction(
      checkList.foreach { chk =>
        val generalConfig = chk.toConfig
        val outerConfig = generalConfig.getConfig("config")

        val id = generalConfig.getString("id")
        val tipo = generalConfig.getString("type")
        val subtype = generalConfig.getString("subtype")
        val description = Try{generalConfig.getString("description")}.toOption

        Check(id,tipo,subtype,description).insert()

        tipo.toUpperCase() match {
          case "SQL" =>
            val db = outerConfig.getString("source")
            val query = outerConfig.getString("query")
            SqlCheck(id,db,query).insert()

          case "SNAPSHOT" =>
            val metric = outerConfig.getStringList("metrics").head
            SnapshotCheck(id,metric).insert()

            Try(outerConfig.getObject("params").foreach(param =>
                CheckParameter(id, param._1, param._2.unwrapped.toString).insert()
            ))

          case "TREND" =>
            val metric = outerConfig.getStringList("metrics").head
            val rule = outerConfig.getString("rule")
            TrendCheck(id,metric,rule).insert()

            Try(outerConfig.getObject("params").foreach(param =>
                CheckParameter(id, param._1, param._2.unwrapped.toString).insert()
            ))
        }

      }
    )
  }

  /**
    * Parses targets from configuration file
    * @return Map of (target_id, target_config)
    */
  private def parseTargets(configObj: Config): Unit = {
    val targetList: List[ConfigObject] = configObj.getObjectList("Targets").toList
    inTransaction(
      targetList.foreach { trg =>

        val outerConf = trg.toConfig
        val tipo = outerConf.getString("type")
        val id = Try {
          outerConf.getString("id")
        }.getOrElse(tipo)

        val inConfig = outerConf.getObject("config").toConfig
        val fileFormat = inConfig.getString("fileFormat")
        val path = inConfig.getString("path")

        val delimiter = Try{inConfig.getString("delimiter")}.toOption
        val savemode = Try{inConfig.getString("savemode")}.toOption
        val date = Try{inConfig.getString("date")}.toOption
        val partitions = Try{inConfig.getInt("partitions")}.toOption

        new Target(id, tipo, fileFormat, path, delimiter, savemode, partitions).insert()
        if (tipo == "SYSTEM") {
          outerConf.getStringList("checkList").toList.foreach(
            check => TargetToChecks(check, id).insert
          )
          outerConf.getStringList("mailingList").toList.foreach(
            mail => Mail(mail, id).insert
          )
        }
      }
    )
  }

}


