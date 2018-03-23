package it.agilelab.bigdata.DataQuality.utils.io

import java.io.IOException

import it.agilelab.bigdata.DataQuality.checks.{CheckFailure, CheckResult}
import it.agilelab.bigdata.DataQuality.exceptions.IllegalParameterException
import it.agilelab.bigdata.DataQuality.metrics._
import it.agilelab.bigdata.DataQuality.targets.{HdfsTargetConfig, SystemTargetConfig, TargetConfig}
import it.agilelab.bigdata.DataQuality.utils.{Logging, _}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FSDataOutputStream, FileSystem, FileUtil, Path}
import org.apache.spark.SparkContext
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.{DataFrame, SQLContext, SaveMode}

/**
  * Created by Gianvito Siciliano on 13/12/16.
  *
  * HDFS writing manager
  */
object HdfsWriter extends Logging {

  def processSystemTarget(conf: TargetConfig,
                          finalCheckResults: Seq[CheckResult])(
      implicit sqlContext: SQLContext,
      fs: FileSystem,
      settings: DQSettings): Unit = {

    val systemConfig: SystemTargetConfig = conf.asInstanceOf[SystemTargetConfig]
    val requestedChecks: Seq[CheckResult] =
      finalCheckResults.filter(x => systemConfig.checkList.contains(x.checkId))

    val numOfFailedChecks: Int = requestedChecks.count(checkRes =>
      checkRes.status == CheckFailure.stringValue)

    systemConfig.outputConfig.getType.toUpperCase match {
      case "HDFS" =>
        val failedCheckIds: String = requestedChecks
          .filter(checkRes => checkRes.status == CheckFailure.stringValue)
          .map(x => x.checkId)
          .mkString(",")

        val hdfsFileConfig =
          systemConfig.outputConfig.asInstanceOf[HdfsTargetConfig]
        this.save(hdfsFileConfig, requestedChecks)

        if (numOfFailedChecks > 0) {
          log.warn(
            s"$numOfFailedChecks of requested check failed. Sending alert email...")
          val fullpath = hdfsFileConfig.path + "/" + hdfsFileConfig.fileName + s"_${settings.refDateString}" + "." + hdfsFileConfig
            .fileFormat

          (settings.mailingMode, settings.mailingConfig) match {
            case ("internal", _) =>
              sendBashMail(numOfFailedChecks,
                           failedCheckIds,
                           fullpath,
                           systemConfig)
            case ("external", Some(mconf)) =>
              sendMail(systemConfig.mailList, None, fullpath)(mconf)
            case (_, _) => log.error("Mailing configuration is incorrect!")
          }
        }
    }
  }

  def saveVirtualSource(source: DataFrame,
                        targetConfig: HdfsTargetConfig,
                        execDate: String)(implicit fs: FileSystem,
                                          sparkContext: SparkContext): Unit = {
    saveCsv(source, targetConfig, execDate)
  }

  /**
    * Function-aggregator to save dataframe in HDFS
    *
    * @param target target configuration
    * @param sq sequence to save
    * @param fs file system
    * @param settings DataQuality configuration
    */
  def save(target: HdfsTargetConfig, sq: Seq[Product with TypedResult])(
      implicit sqlContext: SQLContext,
      fs: FileSystem,
      settings: DQSettings): Unit = {
    log.info(s"starting 'write ${target.fileName.toUpperCase} results' ")

    if (sq.nonEmpty) {
      // since we want to allow you to save on the custom date
      val execDate: String = settings.refDateString

      val df = sq.head.getType match {
        case DQResultTypes.column =>
          val df =
            sqlContext.createDataFrame(sq.asInstanceOf[Seq[ColumnMetricResult]])
          df.withColumn("temp", df("columnNames").cast(StringType))
            .drop("columnNames")
            .withColumnRenamed("temp", "columnNames")
        case DQResultTypes.file =>
          sqlContext.createDataFrame(sq.asInstanceOf[Seq[FileMetricResult]])
        case DQResultTypes.composed =>
          sqlContext.createDataFrame(sq.asInstanceOf[Seq[ComposedMetricResult]])
        case DQResultTypes.check =>
          sqlContext.createDataFrame(sq.asInstanceOf[Seq[CheckResult]])
        case x => throw IllegalParameterException(x.toString)
      }

      target.fileFormat.toUpperCase match {
        case "CSV" | "TXT" =>
          saveCsv(df, target, target.date.getOrElse(execDate))(
            fs,
            sqlContext.sparkContext)
        case "PARQUET" =>
          saveParquet(df, target, target.date.getOrElse(execDate))
        case _ => throw IllegalParameterException(target.fileFormat.toUpperCase)
      }

    } else log.warn("Failed to write an empty file")
  }

  /**
    * Saves CSV file with results
    * @param df data frame to save
    * @param targetConfig target configuration
    * @param execDate save date
    * @param fs file system
    */
  private def saveCsv(df: DataFrame,
                      targetConfig: HdfsTargetConfig,
                      execDate: String)(implicit fs: FileSystem,
                                        sparkContext: SparkContext): Unit = {
    log.debug("path: " + targetConfig.path)

    val tempFileName = targetConfig.path + "/" + targetConfig.fileName + ".tmp" //-${targetConfig.subType}
    val fileName = targetConfig.path + "/" + targetConfig.fileName + "." + targetConfig.fileFormat //-${targetConfig.subType}

    log.info("writing temp csv file: " + tempFileName)

    val header: String =
      if (targetConfig.quoted) {
        df.write
          .format("com.databricks.spark.csv")
          .option("header", "false")
          .option("quoteMode", "ALL")
          .option("delimiter", targetConfig.delimiter.getOrElse("|"))
          .option("nullValue", "")
          .mode(SaveMode.Overwrite)
          .save(tempFileName)

        df.schema.fieldNames.mkString(
          "\"",
          "\"" + s"${targetConfig.delimiter.getOrElse("|").toString}" + "\"",
          "\"")
      } else {
        df.write
          .format("com.databricks.spark.csv")
          .option("header", "false")
          //      .option("quoteMode", "ALL")
          .option("delimiter", targetConfig.delimiter.getOrElse("|"))
          .option("nullValue", "")
          .mode(SaveMode.Overwrite)
          .save(tempFileName)

        df.schema.fieldNames.mkString(targetConfig.delimiter.getOrElse("|"))
      }

    log.info("temp csv file: " + tempFileName + " written")
    try {
      val path = new Path(fileName)
      if (fs.exists(path)) fs.delete(path, false)
      val headerOutputStream: FSDataOutputStream =
        fs.create(new Path(tempFileName + "/header"))
      headerOutputStream.writeBytes(header + "\n")
      headerOutputStream.close()
      FileUtil.copyMerge(fs,
                         new Path(tempFileName),
                         fs,
                         path,
                         true,
                         new Configuration(),
                         null)
    } catch {
      case ioe: IOException => log.warn(ioe)
    }

    log.info("final csv file: " + fileName + " merged")
    log.info("'write output' step finished")
  }

  /**
    * Save Parquet file to the HDFS
    * @param df data frame to save
    * @param targetConfig target configuration
    * @param execDate save date
    * @param fs file system
    */
  private def saveParquet(df: DataFrame,
                          targetConfig: HdfsTargetConfig,
                          execDate: String)(implicit fs: FileSystem): Unit = {
    log.info(s"starting 'write ${targetConfig.fileName.toUpperCase} results' ")
    log.debug("path: " + targetConfig.path)

    val tempFileName = targetConfig.path + "/" + targetConfig.fileName + s"_$execDate" + ".tmp" //-${targetConfig.subType}
    val fileName = targetConfig.path + "/" + targetConfig.fileName + s"_$execDate" + "." + targetConfig.fileFormat //-${targetConfig.subType}

    log.info("writing temp parquet file: " + tempFileName)
    df.coalesce(1)
      .write
      .mode(SaveMode.Overwrite)
      .parquet(tempFileName)

    log.info("temp parquet file: " + tempFileName + " written")

    FileUtil.copyMerge(fs,
                       new Path(tempFileName),
                       fs,
                       new Path(fileName),
                       true,
                       new Configuration(),
                       null)

    log.info("final parquet file: " + fileName + " merged")
    log.info("'write output' step finished")
  }
}
