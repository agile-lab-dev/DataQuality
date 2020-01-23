package it.agilelab.bigdata.DataQuality.postprocessors

import java.util

import com.typesafe.config.Config
import it.agilelab.bigdata.DataQuality.checks.CheckResult
import it.agilelab.bigdata.DataQuality.exceptions.IllegalParameterException
import it.agilelab.bigdata.DataQuality.metrics.MetricResult
import it.agilelab.bigdata.DataQuality.sources.HdfsFile
import it.agilelab.bigdata.DataQuality.targets.HdfsTargetConfig
import it.agilelab.bigdata.DataQuality.utils
import it.agilelab.bigdata.DataQuality.utils.DQSettings
import it.agilelab.bigdata.DataQuality.utils.io.{HdfsReader, HdfsWriter}
import org.apache.hadoop.fs.FileSystem
import org.apache.spark.sql.functions.lit
import org.apache.spark.sql.{DataFrame, SQLContext}

import scala.collection.JavaConversions._
import scala.util.Try

final class EnrichPostprocessor(config: Config, settings: DQSettings)
    extends BasicPostprocessor(config, settings) {

  private val vs: Option[String] = Try(config.getString("source")).toOption
  private val metrics: util.List[String] = config.getStringList("metrics")
  private val checks: util.List[String] = config.getStringList("checks")
  private val extra = config.getObject("extra").toMap

  private val target: HdfsTargetConfig = {
    val conf = config.getConfig("saveTo")
    utils.parseTargetConfig(conf)(settings).get
  }

  override def process(vsRef: Set[HdfsFile],
                       metRes: Seq[MetricResult],
                       chkRes: Seq[CheckResult])(
      implicit fs: FileSystem,
      sqlContext: SQLContext,
      settings: DQSettings): HdfsFile = {

    import sqlContext.implicits._

    val df: DataFrame = vs match {
      case Some(vsource) =>
        val reqVS: HdfsFile = vsRef.filter(vr => vr.id == vsource).head
        HdfsReader.load(reqVS, settings.ref_date).head
      case None =>
        sqlContext.sparkContext.parallelize(Seq(1)).toDF("teapot")
    }

    val reqMet: Seq[(String, Double)] = metRes
      .filter(mr => metrics.contains(mr.metricId))
      .map(mr => mr.metricId -> mr.result)
    val reqCheck: Seq[(String, String)] = chkRes
      .filter(cr => checks.contains(cr.checkId))
      .map(cr => cr.checkId -> cr.status)

    if (reqMet.size != metrics.size())
      throw IllegalParameterException("Some of stated metrics are missing!")
    if (reqCheck.size != checks.size())
      throw IllegalParameterException("Some of stated checks are missing!")

    val dfWithMet: DataFrame =
      reqMet.foldLeft(df)((df, met) => df.withColumn(met._1, lit(met._2)))
    val dfWithChecks = reqCheck.foldLeft(dfWithMet)((df, met) =>
      df.withColumn(met._1, lit(met._2)))
    val dfWithExtra = extra.foldLeft(dfWithChecks)((df, ex) =>
      df.withColumn(ex._1, lit(ex._2.unwrapped())))

    HdfsWriter.saveVirtualSource(
      dfWithExtra.drop("teapot"),
      target,
      settings.refDateString)(fs, sqlContext.sparkContext)

    new HdfsFile(target)
  }
}
