package it.agilelab.bigdata.DataQuality.sources

import java.sql.Connection

import com.typesafe.config.Config
import it.agilelab.bigdata.DataQuality.exceptions.IllegalParameterException
import it.agilelab.bigdata.DataQuality.utils
import it.agilelab.bigdata.DataQuality.utils.io.db.readers.{ORCLReader, PostgresReader, SQLiteReader, TableReader}
import org.apache.spark.sql.{DataFrame, SQLContext}

import scala.util.Try

/**
  * Created by Egor Makhov on 09/06/2017.
  *
  * Representation of database configuration. Unified for all types of databases
  */
case class DatabaseConfig(
                           id: String,
                           subtype: String,
                           host: String,
                           port: Option[String],
                           service: Option[String],
                           user: Option[String],
                           password: Option[String],
                           schema: Option[String]
                         ) {

  // Constructor for
  def this(config: Config) = {
    this(
      Try(config.getString("id")).getOrElse(""),
      config.getString("subtype"),
      config.getString("host"),
      Try(config.getString("port")).toOption,
      Try(config.getString("service")).toOption,
      Try(config.getString("user")).toOption,
      Try(config.getString("password")).toOption,
      Try(config.getString("schema")).toOption
    )
  }

  private val dbReader: TableReader = subtype match {
    case "ORACLE"   => ORCLReader(this)
    case "SQLITE"   => SQLiteReader(this)
    case "POSTGRES" => PostgresReader(this)
    case x          => throw IllegalParameterException(x)
  }

  def getConnection: Connection = dbReader.getConnection
  def getUrl: String = dbReader.getUrl
  // the trick here is that table credentials can be different from database one,
  // so that function allow you to connect to the database with multiple credentials
  // without specification of multiple databases
  def loadData(table: String,
               user: Option[String] = this.user,
               password: Option[String] = this.password)(
                implicit sqlContext: SQLContext): DataFrame =
    dbReader.loadData(utils.makeTableName(schema, table), user, password)
}
