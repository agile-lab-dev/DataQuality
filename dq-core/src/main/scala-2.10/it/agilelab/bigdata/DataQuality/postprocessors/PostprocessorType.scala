package it.agilelab.bigdata.DataQuality.postprocessors

import com.typesafe.config.Config
import it.agilelab.bigdata.DataQuality.checks.CheckResult
import it.agilelab.bigdata.DataQuality.metrics.MetricResult
import it.agilelab.bigdata.DataQuality.sources.HdfsFile
import it.agilelab.bigdata.DataQuality.utils.DQSettings
import org.apache.hadoop.fs.FileSystem
import org.apache.spark.sql.SQLContext

object PostprocessorType extends Enumeration {
  val enrich: PostprocessorVal =
    PostprocessorVal("enrich", classOf[EnrichPostprocessor])
  val transpose: PostprocessorVal =
    PostprocessorVal("transpose_by_key", classOf[TransposePostprocessor])
  val headless: PostprocessorVal =
    PostprocessorVal("transpose_by_column", classOf[TransposeByColumnPostprocessor])
  val arrange: PostprocessorVal =
    PostprocessorVal("arrange", classOf[ArrangePostprocessor])

  protected case class PostprocessorVal(name: String,
                                        service: Class[_ <: BasicPostprocessor])
    extends super.Val() {
    override def toString(): String = this.name
  }
  implicit def convert(value: Value): PostprocessorVal =
    value.asInstanceOf[PostprocessorVal]
}

abstract class BasicPostprocessor(config: Config) {
  def process(vsRef: Set[HdfsFile],
              metRes: Seq[MetricResult],
              chkRes: Seq[CheckResult])(implicit fs: FileSystem,
                                        sqlContext: SQLContext,
                                        settings: DQSettings): HdfsFile
}
