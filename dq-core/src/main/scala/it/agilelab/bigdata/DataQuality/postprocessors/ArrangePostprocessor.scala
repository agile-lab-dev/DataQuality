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

  private case class ColumnSelector(name: String, tipo: Option[String] = None, format: Option[String] = None, precision: Option[Integer] = None) {
    def toColumn()(implicit df: DataFrame): Column = {

      val dataType: Option[NumericType with Product with Serializable] =
        tipo.getOrElse("").toUpperCase match {
          case "DOUBLE" => Some(DoubleType)
          case "INT"    => Some(IntegerType)
          case "LONG"   => Some(LongType)
          case _        => None
        }

      import org.apache.spark.sql.functions.format_number
      import org.apache.spark.sql.functions.format_string

      (dataType, precision, format) match {
        case (Some(dt), None, None) => df(name).cast(dt)
        case(Some(dt), None, Some(f)) => format_string(f, df(name).cast(dt)).alias(name)
        case (Some(dt), Some(p),None) => format_number(df(name).cast(dt), p).alias(name)
        case (None, Some(p), None) => format_number(df(name), p).alias(name)
        case (None, None, Some(f)) => format_string(f, df(name)).alias(name)
        case _ => df(name)
      }
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
      case x: java.util.HashMap[_, String] => {
        val (name, v) = x.head.asInstanceOf[String Tuple2 _]

        v match {
          case v: String =>
            ColumnSelector(name, Option(v))
          case v: java.util.HashMap[String, _] => {
            val k = v.head._1
            val f = v.head._2

            f match {
              case f: Integer =>
                ColumnSelector(name, Option(k), None, Option(f))
              case f: String =>
                ColumnSelector(name, Option(k), Option(f))
            }
          }
        }
      }
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
