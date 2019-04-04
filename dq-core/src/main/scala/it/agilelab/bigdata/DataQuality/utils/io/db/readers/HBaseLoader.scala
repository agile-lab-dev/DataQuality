package it.agilelab.bigdata.DataQuality.utils.io.db.readers

import it.agilelab.bigdata.DataQuality.sources.HBaseSrcConfig
import org.apache.spark.sql.{DataFrame, Row, SQLContext}
import it.nerdammer.spark.hbase._
import it.nerdammer.spark.hbase.conversion.FieldReader
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.{StringType, StructField, StructType}


object HBaseLoader {

  private def seqReader[T]()(implicit m1: FieldReader[Option[T]]): FieldReader[Seq[Option[T]]] = new FieldReader[Seq[Option[T]]] {
    def map(data: HBaseData): Seq[Option[T]] = data.map(optArr => m1.map(Iterable(optArr))).toSeq
  }

  /**
    * Loads table from HBase and cast it to DataFrame.
    *
    * @param conf       hbase loading configuration
    * @param sqlContext SQL Context of the spark application
    * @return
    */
  def loadToDF(conf: HBaseSrcConfig)(implicit sqlContext: SQLContext): DataFrame = {

    implicit val stringSeqReader: FieldReader[Seq[Option[String]]] = seqReader[String]()

    val header: Seq[String] = Seq("key") ++ conf.hbaseColumns
    val rdd: RDD[Row] =
      sqlContext.sparkContext
        .hbaseTable[Seq[Option[String]]](conf.table)
        .select(conf.hbaseColumns: _*)
        .map {
          case (s: Seq[Option[String]]) => Row.fromSeq(s)
        }
    val struct = StructType(header.map(StructField(_, StringType)))
    sqlContext.createDataFrame(rdd, struct)
  }

}
