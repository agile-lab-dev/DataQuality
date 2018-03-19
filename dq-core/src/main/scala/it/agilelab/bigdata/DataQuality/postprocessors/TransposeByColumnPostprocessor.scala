package it.agilelab.bigdata.DataQuality.postprocessors

import com.typesafe.config.Config
import it.agilelab.bigdata.DataQuality.checks.CheckResult
import it.agilelab.bigdata.DataQuality.metrics.MetricResult
import it.agilelab.bigdata.DataQuality.sources.HdfsFile
import it.agilelab.bigdata.DataQuality.targets.HdfsTargetConfig
import it.agilelab.bigdata.DataQuality.utils
import it.agilelab.bigdata.DataQuality.utils.DQSettings
import it.agilelab.bigdata.DataQuality.utils.io.{HdfsReader, HdfsWriter}
import org.apache.hadoop.fs.FileSystem
import org.apache.spark.sql.functions.{col, lit}
import org.apache.spark.sql.{DataFrame, SQLContext}

import scala.util.Try

final class TransposeByColumnPostprocessor(config: Config)
    extends BasicPostprocessor(config) {

  import scala.collection.JavaConverters._

  private val vs = config.getString("source")
  private val target: HdfsTargetConfig = {
    val conf = config.getConfig("saveTo")
    utils.parseTargetConfig(conf).get
  }
  private val numOfColumns
    : Option[Int] = Try(config.getInt("numberOfColumns")).toOption

  private val keys: Option[Array[String]] = Try {
    config.getStringList("keyColumns").asScala.toArray[String]
  }.toOption
  private val offset: Int = 1

  override def process(vsRef: Set[HdfsFile],
                       metRes: Seq[MetricResult],
                       chkRes: Seq[CheckResult])(
      implicit fs: FileSystem,
      sqlContext: SQLContext,
      settings: DQSettings): HdfsFile = {

    val reqVS: HdfsFile = vsRef.filter(vr => vr.id == vs).head
    val df: DataFrame = HdfsReader.load(reqVS, settings.ref_date).head

    val (colsToProcess: Array[String], colsToRemain: Array[String]) =
      keys match {
        case Some(ks) => (ks, df.columns.filterNot(ks.contains))
        case None     => (df.columns, Array.empty[String])
      }

    val headless: DataFrame = numOfColumns match {
      case Some(x) if x > colsToProcess.length =>
        val cols: Array[String] = colsToProcess
        val hlCols: Seq[String] =
          (0 until x).foldLeft(Seq.empty[String])((arr, i) =>
            arr ++ Seq(s"KEY_${i + offset}", s"VALUE_${i + offset}"))

        val columnDF = cols.zipWithIndex.foldLeft(df) {
          case (curr, (col, i)) =>
            curr
              .withColumn(s"KEY_${i + offset}", lit(col))
              .withColumnRenamed(col, s"VALUE_${i + offset}")
        }
        (cols.length until x)
          .foldLeft(columnDF) {
            case (curr, i) =>
              curr
                .withColumn(s"KEY_${i + offset}", lit(""))
                .withColumn(s"VALUE_${i + offset}", lit(""))
          }
          .select((hlCols ++ colsToRemain).map(col): _*)
      case Some(x) if x <= colsToProcess.length =>
        val cols = colsToProcess.slice(0, x)
        val hlCols: Seq[String] =
          cols.indices.foldLeft(Seq.empty[String])((arr, i) =>
            arr ++ Seq(s"KEY_${i + offset}", s"VALUE_${i + offset}"))
        cols.zipWithIndex
          .foldLeft(df) {
            case (curr, (col, i)) =>
              curr
                .withColumn(s"KEY_${i + offset}", lit(col))
                .withColumnRenamed(col, s"VALUE_${i + offset}")
          }
          .select((hlCols ++ colsToRemain).map(col): _*)
      case None =>
        val cols = colsToProcess
        val hlCols: Seq[String] =
          cols.indices.foldLeft(Seq.empty[String])((arr, i) =>
            arr ++ Seq(s"KEY_${i + offset}", s"VALUE_${i + offset}"))
        cols.zipWithIndex
          .foldLeft(df) {
            case (curr, (col, i)) =>
              curr
                .withColumn(s"KEY_${i + offset}", lit(col))
                .withColumnRenamed(col, s"VALUE_${i + offset}")
          }
          .select((hlCols ++ colsToRemain).map(col): _*)
    }

    HdfsWriter.saveVirtualSource(headless, target, settings.refDateString)(
      fs,
      sqlContext.sparkContext)

    new HdfsFile(target)
  }
}
