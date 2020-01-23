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
import org.apache.spark.sql.types.{DoubleType, IntegerType, LongType, NumericType}
import org.apache.spark.sql.{Column, DataFrame, SQLContext}

import scala.collection.JavaConversions._

final class ArrangePostprocessor(config: Config, settings: DQSettings)
    extends BasicPostprocessor(config, settings) {

  private case class ColumnSelector(name: String, tipo: String = "") {
    def toColumn()(implicit df: DataFrame): Column = {
      val dataType: Option[NumericType with Product with Serializable] =
        tipo.toUpperCase match {
          case "DOUBLE" => Some(DoubleType)
          case "INT"    => Some(IntegerType)
          case "LONG"   => Some(LongType)
          case _        => None
        }
      if (dataType.isDefined) df(name).cast(dataType.get) else df(name)
    }
  }

  private val vs = config.getString("source")
  private val target: HdfsTargetConfig = {
    val conf = config.getConfig("saveTo")
    utils.parseTargetConfig(conf)(settings).get
  }

  private val columns: Seq[ColumnSelector] =
    config.getAnyRefList("columnOrder").map {
      case x: String => ColumnSelector(x)
      case x: java.util.HashMap[_, _] =>
        val (name, tipo) = x.head.asInstanceOf[String Tuple2 String]
        ColumnSelector(name, tipo)
    }

  override def process(vsRef: Set[HdfsFile],
                       metRes: Seq[MetricResult],
                       chkRes: Seq[CheckResult])(
      implicit fs: FileSystem,
      sqlContext: SQLContext,
      settings: DQSettings): HdfsFile = {

    val reqVS: HdfsFile = vsRef.filter(vr => vr.id == vs).head
    implicit val df: DataFrame = HdfsReader.load(reqVS, settings.ref_date).head

    val arrangeDF = df.select(columns.map(_.toColumn): _*)

    HdfsWriter.saveVirtualSource(arrangeDF, target, settings.refDateString)(
      fs,
      sqlContext.sparkContext)

    new HdfsFile(target)
  }
}
