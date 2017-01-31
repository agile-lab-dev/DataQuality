package it.agilelab.bigdata.DataQuality.utils.io

import java.text.SimpleDateFormat
import java.util.Calendar

import it.agilelab.bigdata.DataQuality.exceptions.IllegalParameterException
import it.agilelab.bigdata.DataQuality.targets.{HdfsTargetConfig, TargetConfig}
import it.agilelab.bigdata.DataQuality.utils.Logging
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, FileUtil, Path}
import org.apache.spark.sql.{DataFrame, SaveMode}

/**
  * Created by Gianvito Siciliano on 13/12/16.
  */
object HdfsWriter extends Logging {


  def save(target: TargetConfig, df: DataFrame)(implicit fs: FileSystem): Unit = {

    val formatDate = new SimpleDateFormat("yyyy-MM-dd:hhmm")
    val now  = Calendar.getInstance().getTime
    val execDate = formatDate.format(now)

    target.getType.toUpperCase match {
      case "HDFS" => {
        val hdfsTarget = target.asInstanceOf[HdfsTargetConfig]
        hdfsTarget.fileFormat.toUpperCase match {
          case "CSV" | "TXT" => saveCsv(df, hdfsTarget, execDate)
          case "PARQUET"     => saveParquet(df, hdfsTarget, execDate)
          case _ => throw new IllegalParameterException(hdfsTarget.fileFormat.toUpperCase)
        }
      }
      case x => throw new IllegalParameterException(x)
    }
  }

  private def saveCsv(df: DataFrame, targetConfig: HdfsTargetConfig,  execDate: String)(implicit fs: FileSystem): Unit = {
    log.info(s"starting 'write ${targetConfig.fileName.toUpperCase} results' ")
    log.debug("path: " + targetConfig.path)

    val tempFileName = targetConfig.path + "/" + targetConfig.fileName + s"_$execDate" + ".tmp" //-${targetConfig.subType}
    val fileName     = targetConfig.path + "/" + targetConfig.fileName + s"_$execDate" + "." +targetConfig.fileFormat  //-${targetConfig.subType}

    val result = df.repartition(targetConfig.partitions)

    log.info("writing temp csv file: " + tempFileName)
    result.write
      .format("com.databricks.spark.csv")
      .option("header", "true")
      .option("delimiter", targetConfig.delimiter.getOrElse("|"))
      .option("nullValue", "")
      .mode(SaveMode.Append)
      .save(tempFileName)


    log.info("temp csv file: " + tempFileName + " written")

    FileUtil.copyMerge(fs, new Path(tempFileName), fs, new Path(fileName), true, new Configuration(), null)

    log.info("final csv file: " + fileName + " merged")
    log.info("'write output' step finished")

  }

  private def saveParquet(df: DataFrame, targetConfig: HdfsTargetConfig, execDate: String)(implicit fs: FileSystem): Unit = {
    log.info(s"starting 'write ${targetConfig.fileName.toUpperCase} results' ")
    log.debug("path: " + targetConfig.path)

    val tempFileName = targetConfig.path + "/" + targetConfig.fileName + s"_$execDate" + ".tmp" //-${targetConfig.subType}
    val fileName     = targetConfig.path + "/" + targetConfig.fileName + s"_$execDate" + "." +targetConfig.fileFormat  //-${targetConfig.subType}

    val result = df.repartition(targetConfig.partitions)


    log.info("writing temp parquet file: " + tempFileName)
    result.write
      .mode(SaveMode.Append)
      .parquet(tempFileName)


    log.info("temp parquet file: " + tempFileName + " written")

    FileUtil.copyMerge(fs, new Path(tempFileName), fs, new Path(fileName), true, new Configuration(), null)

    log.info("final parquet file: " + fileName + " merged")
    log.info("'write output' step finished")
  }


}

