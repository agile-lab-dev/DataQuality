package it.agilelab.bigdata.DataQuality

import java.io.File
import java.sql.ResultSet
import java.text.DecimalFormat

import com.typesafe.config.Config
import it.agilelab.bigdata.DataQuality.metrics.MetricProcessor.ParamMap
import it.agilelab.bigdata.DataQuality.metrics.{ColumnMetricResult, ComposedMetricResult, FileMetricResult}
import it.agilelab.bigdata.DataQuality.targets.{HdfsTargetConfig, SystemTargetConfig}
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.SparkContext
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

import scala.collection.immutable.TreeMap
import scala.collection.mutable
import scala.util.Try
import scala.util.parsing.json.JSONObject

/**
  * Created by Egor Makhov on 04/05/2017.
  */
package object utils extends Logging {

  // Application parameters
  val applicationDateFormat: String = "yyyy-MM-dd"
  val doubleFractionFormat: Int = 13
  val shortDateFormatter: DateTimeFormatter =
    DateTimeFormat.forPattern("yyyyMMdd")

  def parseTargetConfig(config: Config): Option[HdfsTargetConfig] = {
    Try {
      val name: Option[String] = Try(config.getString("fileName")).toOption
      val format = config.getString("fileFormat")
      val path = config.getString("path")
      val delimiter = Try(config.getString("delimiter")).toOption
      val quoted: Boolean = Try(config.getBoolean("quoted")).getOrElse(false)
      HdfsTargetConfig(name.getOrElse(""),
                       format,
                       path,
                       delimiter,
                       quoted = quoted)
    }.toOption
  }

  def saveErrors(header: Seq[String], content: (String, mutable.Seq[String]))(
      implicit sparkContext: SparkContext,
      settings: DQSettings): Unit = {

    val hc = sparkContext.hadoopConfiguration
    val fs = FileSystem.get(hc)
    val dateString = settings.ref_date.toString(shortDateFormatter)
    val basePath = settings.errorFolderPath.getOrElse("")
    val finalPath = s"$basePath/$dateString/${content._1}.csv"
    val separator: Char = ','

    val headerString = "METRIC_ID" + header.zipWithIndex.foldLeft("") {
      (base, n) =>
        base + s"${separator}COLUMN_${n._2 + 1}${separator}VALUE_${n._2 + 1}"
    }

    val errorFile = fs.create(new Path(finalPath))
    try {
      errorFile write (headerString + "\n").getBytes("UTF-8")
      val (metric, errors) = content
      errors.foreach { erStr =>
        val er = erStr.split(",")
        val csvString: String = metric + er
          .zip(header)
          .foldLeft("")(
            (base, n) => base + s"$separator${n._2}$separator${n._1}"
          )
        errorFile write (csvString + "\n").getBytes("UTF-8")
      }
    } catch {
      case e: Exception =>
        log.warn(s"Some error occurred while writing $finalPath")
        log.warn(e.toString)
    } finally {
      errorFile.close()
    }
  }

  def sendMail(recievers: Seq[String], text: Option[String], filepath: String)(
      implicit mailer: Mailer): Unit = {

    val defaultText =
      "Some of requested checks failed. Please, check attached csv."

    Mail a Mail(
      from = (mailer.address, "AgileLAB DataQuality"),
      to = recievers,
      subject = "Data Quality failed check alert",
      message = text.getOrElse(defaultText),
      attachment = Try(new File(filepath)).toOption
    )

  }

  def sendBashMail(
      numOfFailedChecks: Int,
      failedCheckIds: String,
      fullPath: String,
      systemConfig: SystemTargetConfig)(implicit settings: DQSettings): Unit = {
    import sys.process.stringSeqToProcess
    val mailList: Seq[String] = systemConfig.mailList
    val mailListString = mailList.mkString(" ")
    val targetName = systemConfig.id

    Seq(
      "/bin/bash",
      settings.appDir + "/sendMail.sh",
      targetName,
      mailListString,
      numOfFailedChecks.toString,
      failedCheckIds,
      fullPath
    ) !!

  }

  /**
    * Generates explicit id for top N metric
    * Used in metric calculator result linking
    * @example generateMetricSubId("TOP_N", 3) => List("TOP_N_3","TOP_N_2", "TOP_N_1")
    * @param id Base id
    * @param n Requested N (amount of results)
    * @param aggr Generated id aggregator
    *
    * @return List of generated ids*/
  def generateMetricSubId(id: String,
                          n: Int,
                          aggr: List[String] = List.empty): List[String] = {
    if (n >= 1) {
      val newId: List[String] = List(id + "_" + n.toString)
      return generateMetricSubId(id, n - 1, aggr ++ newId)
    }
    aggr
  }

  /**
    * Generates tail for metric from parameter map
    * Used in metric calculator result linking
    * @example getParametrizedMetricTail(Map("targetValue"->1, "accuracy"->0.01d)) => ":0.01:1"
    * @param paramMap parameter map to process
    *
    * @return Generated result tail
    */
  def getParametrizedMetricTail(paramMap: ParamMap): String = {
    if (paramMap.nonEmpty) {
      // sorted by key to return the same result without affect of the map key order
      val sorted = TreeMap(paramMap.toArray: _*)
      val tail = sorted.values.toList.mkString(":", ":", "")
      return tail
    }
    ""
  }

  /**
    * Maps map to JSON
    * Used to save the paramMap in local DB
    * @param map parameter map
    *
    * @return JSON representation of parameter map
    */
  def mapToJsonString(map: Map[String, Any]): String = {
    if (map.isEmpty) return ""
    JSONObject(map).toString
  }

  /**
    * Formats double. Used in result saving
    * @param double Target double
    *
    * @return String representation of formatted double
    */
  def formatDouble(double: Double): String = {
    val format: DecimalFormat = new DecimalFormat()
    // format can be changed
    format.setMaximumFractionDigits(utils.doubleFractionFormat)
    format.format(double)
  }

  /**
    * Map JDBC ResultSet to Seq of ColumnMetricResult
    * Used in trend check processing
    * WARNING Seq is lazy evaluated
    * @param rs provided result set
    *
    * @return Lazy seq of ColMetResults
    */
  def mapResToColumnMet(rs: ResultSet): Seq[ColumnMetricResult] = {
    new Iterator[ColumnMetricResult] {
      def hasNext: Boolean = rs.next()
      def next() = {

        val columIds = rs.getArray(5).getArray.asInstanceOf[Array[String]]
        ColumnMetricResult(
          rs.getString(1),
          rs.getString(2),
          rs.getString(3),
          rs.getString(4),
          columIds.toSeq,
          rs.getString(6),
          rs.getDouble(7),
          rs.getString(8)
        )

      }

    }.toSeq
  }

  /**
    * Map JDBC ResultSet to Seq of ComposedMetricResult
    * Used in trend check processing
    * WARNING Seq is lazy evaluated
    * @param rs provided result set
    *
    * @return Lazy seq of ComposedMetricResults
    */
  def mapResToComposedMet(rs: ResultSet): Seq[ComposedMetricResult] = {
    new Iterator[ComposedMetricResult] {
      def hasNext: Boolean = rs.next()
      def next() = ComposedMetricResult(
        rs.getString(1),
        rs.getString(2),
        rs.getString(3),
        rs.getString(4),
        rs.getString(5),
        rs.getDouble(6),
        rs.getString(7)
      )
    }.toSeq
  }

  /**
    * Map JDBC ResultSet to Seq of FileMetricResult
    * Used in trend check processing
    * WARNING Seq is lazy evaluated
    * @param rs provided result set
    *
    * @return Lazy seq of FileMetResults
    */
  def mapResToFileMet(rs: ResultSet): Seq[FileMetricResult] = {
    new Iterator[FileMetricResult] {
      def hasNext: Boolean = rs.next()
      def next() = FileMetricResult(
        rs.getString(1),
        rs.getString(2),
        rs.getString(3),
        rs.getString(4),
        rs.getDouble(5),
        rs.getString(6)
      )
    }.toSeq
  }

  /**
    * Tries to cast any value to String
    * Used in metric calculators
    * @param value value to cast
    *
    * @return Option of String. If it's null return None
    */
  def tryToString(value: Any): Option[String] = {
    value match {
      case null      => None
      case x: String => Some(x)
      case x         => Try(x.toString).toOption
    }
  }

  private val binaryCasting: (Array[Byte]) => Option[Double] =
    (bytes: Array[Byte]) => {
      println(bytes)
      if (bytes == null) None
      else {
        val casted = Option(bytes.map(b => b.toChar).mkString.toDouble)
        println(casted)
        casted
      }
    }

  /**
    * Tries to cast any value to Double
    * Used in metric calculators
    * @param value value to cast
    *
    * @return Option of Double. If it's null return None
    */
  def tryToDouble(value: Any): Option[Double] = {
    value match {
      case null           => None
      case x: Double      => Some(x)
      case x: Array[Byte] => binaryCasting(x)
      case x              => Try(x.toString.toDouble).toOption
    }
  }

  def camelToUnderscores(name: String): String =
    "[A-Z\\d]".r.replaceAllIn(name, { m =>
      if (m.end(0) == 1) {
        m.group(0).toLowerCase()
      } else {
        "_" + m.group(0).toLowerCase()
      }
    })

  def makeTableName(schema: Option[String], table: String): String = {
    val sep: String = "."
    schema match {
      case Some(x) => x + sep + table
      case None => table
    }
  }

}
