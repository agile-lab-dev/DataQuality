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
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SQLContext}

import scala.collection.JavaConversions._

final class TransposePostprocessor(config: Config, settings: DQSettings)
    extends BasicPostprocessor(config, settings: DQSettings) {
  private val vs = config.getString("source")
  private val keys = config.getStringList("keyColumns")
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

    def toLong(df: DataFrame, by: Seq[String]): DataFrame = {
      val (cols, types) = df.dtypes.filter { case (c, _) => !by.contains(c) }.unzip
      require(types.distinct.length == 1)

      val kvs = explode(
        array(
          cols.map(c => struct(lit(c).alias(settings.backComp.trKeyName), col(c).alias(settings.backComp.trValueName))): _*
        ))

      val byExprs = by.map(col)

      df.select(byExprs :+ kvs.alias("_kvs"): _*)
        .select(byExprs ++ Seq($"_kvs.${settings.backComp.trKeyName}", $"_kvs.${settings.backComp.trValueName}"): _*)
    }

    val reqVS: HdfsFile = vsRef.filter(vr => vr.id == vs).head
    val df: DataFrame = HdfsReader.load(reqVS, settings.ref_date).head

    val transposed: DataFrame = toLong(df, keys)

    HdfsWriter.saveVirtualSource(transposed, target, settings.refDateString)(
      fs,
      sqlContext.sparkContext)

    new HdfsFile(target)
  }

}
