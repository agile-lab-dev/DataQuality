package it.agilelab.bigdata.DataQuality.utils.io

import java.sql.{Array, Connection, ResultSet}

import it.agilelab.bigdata.DataQuality.metrics.ComposedMetricResult
import it.agilelab.bigdata.DataQuality.sources.DatabaseConfig
import it.agilelab.bigdata.DataQuality.utils
import it.agilelab.bigdata.DataQuality.utils.DQSettings
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

import scala.reflect.runtime.universe._
import it.agilelab.bigdata.DataQuality.exceptions.IllegalParameterException
import it.agilelab.bigdata.DataQuality.metrics.{ColumnMetricResult, FileMetricResult}
import it.agilelab.bigdata.DataQuality.utils.{Logging, camelToUnderscores}

/**
  * Created by Egor Makhov on 04/05/2017.
  *
  * Manager to do operations with history database.
  */
class HistoryDBManager(settings: DQSettings) extends Logging {

  private val dbConfig: Option[DatabaseConfig] = settings.resStorage
  private val connection: Option[Connection] = if (dbConfig.isDefined) Some(dbConfig.get.getConnection) else None

  /**
    * Saves metric results to a specific table
    * Main idea is that method is class independent. It's automatically get the class fields and creates
    * JDBC statement to fill and execute.
    * @param metrics Sequence of metric results
    * @param tb Target table
    */
  def saveResultsToDB(metrics: Seq[AnyRef], tb: String): Unit = {
    if (dbConfig.isDefined && connection.isDefined) {
      val table = utils.makeTableName(dbConfig.get.schema, tb)
      log.info(s"Saving '$table'")
      try {
        val fieldNames = metrics.head.getClass.getDeclaredFields
        val fieldStructString: String = fieldNames
          .map(field => camelToUnderscores(field.getName))
          .mkString(" (", ", ", ") ")
        val paramString: String =
          List.fill(fieldNames.length)("?").mkString(" (", ", ", ")")

        val insertSql = "INSERT INTO " + table + fieldStructString + "VALUES" + paramString

        val statement = connection.get.prepareStatement(insertSql)

        metrics.foreach(res => {
          fieldNames
            .zip(Stream from 1)
            .foreach(f => {
              f._1.setAccessible(true)
              val value: Any = f._1.get(res)
              value match {
                // todo: add more formats
                //          case x: Int => statement.setInt(f._2, x)
                case x: Seq[_] =>
                  val xs: Seq[String] = x
                    .filter(_ match {
                      case _: String => true
                      case _         => false
                    })
                    .asInstanceOf[Seq[String]]
                  val array: Array = connection.get.createArrayOf("text", xs.toArray)
                  statement.setArray(f._2, array)
                case x => statement.setString(f._2, x.toString)

              }
            })
          statement.addBatch()
        })

        statement.executeBatch()
        log.info("Success!")
      } catch {
        case _: NoSuchElementException =>
          log.warn("Nothing to save!")
        case e: Exception =>
          log.error("Failed with error:")
          log.error(e.toString)
      }
    } else log.warn("History database is not connected. Avoiding saving the results...")

  }

  /**
    * Loads metric results of previous runs from the local SQLite database
    * Used in trend check processing. On call you should provide only the type of result you wanna get,
    * method will automatically select the proper table
    * @param metricSet Set of requested metrics (set of their ids)
    * @param rule Rule for result selection. Should be "date" or "record"
    * @param tw Requested time window
    *           For "date" selection rule will select from a window [startDate - tw * days, startDate]
    *           For "record" selection rule will select tw records from before starting date
    * @param startDate Start date to selection. Keep in mind that all the time windows are retrospective,
    *                  so the start date is actually a latest one
    * @param f Mapping function (maps resultSet to Seq[MetResults]). Those function provided in the utils package
    *          That param assumes that in some cases you will want to cast result in some specific way.
    * @tparam T Requested result type
    * @return lazy sequence of Metric results
    */
  def loadResults[T: TypeTag](metricSet: List[String],
                              rule: String,
                              tw: Int,
                              startDate: String = settings.refDateString)(
                               f: ResultSet => Seq[T]): Seq[T] = {
    if (dbConfig.isDefined && connection.isDefined) {
      val tb: String = typeOf[T] match {
        case t if t =:= typeOf[ColumnMetricResult]   => "results_metric_columnar"
        case t if t =:= typeOf[FileMetricResult]     => "results_metric_file"
        case t if t =:= typeOf[ComposedMetricResult] => "results_metric_composed"
        case x                                       => throw IllegalParameterException(x.toString)
      }
      val table = utils.makeTableName(dbConfig.get.schema, tb)

      log.info(s"Loading results from $table...")

      val metricIdString =
        List.fill(metricSet.length)("?").mkString("(", ", ", ")")

      val selectSQL = rule match {
        case "record" =>
          // looks a bit bulky and, probably, there is a way to do the same completely with JDBC statements
          s"SELECT * FROM $table WHERE metric_id IN $metricIdString AND source_date <= '$startDate' ORDER BY source_date DESC LIMIT ${tw * metricSet.length}"
        case "date" =>
          // formatter based on joda time
          val formatter: DateTimeFormatter =
            DateTimeFormat.forPattern(utils.applicationDateFormat)
          val lastDate =
            formatter.parseDateTime(startDate).minusDays(tw).toString(formatter)
          s"SELECT * FROM $table WHERE metric_id IN $metricIdString AND source_date >= '$lastDate' AND source_date <= '$startDate'"
        case x => throw IllegalParameterException(x)
      }

      val statement = connection.get.prepareStatement(selectSQL)

      metricSet.zip(Stream from 1).foreach(x => statement.setString(x._2, x._1))
      val results: ResultSet = statement.executeQuery()

      val resSet = f(results)
      log.info(s"Results found: ${resSet.length}")
      resSet
    } else {
      log.warn("History database is not connected. Providing empty historical results...")
      Seq.empty[T]
    }
  }

  /**
    * Closes connection with the local database (obviously)
    * Defined since the connection is private
    */
  def closeConnection(): Unit = {
    if (connection.isDefined) connection.get.close()
  }
}
