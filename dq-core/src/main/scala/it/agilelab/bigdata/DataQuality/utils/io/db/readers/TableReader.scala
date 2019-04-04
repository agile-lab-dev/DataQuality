package it.agilelab.bigdata.DataQuality.utils.io.db.readers

import java.sql.{Connection, ResultSet}
import java.util.Properties

import org.apache.spark.sql.{DataFrame, SQLContext}

/**
  * Created by Egor Makhov on 24/05/2017.
  *
  * Base for all database readers
  */
trait TableReader {

  protected val connectionUrl: String

  /**
    * Runs query and maps result to the desired type
    * Used in SQL checks
    * @param query query to run
    * @param transformOutput transformation function
    * @tparam T desired type
    * @return object of desired type
    */
  def runQuery[T](query: String, transformOutput: ResultSet => T): T

  def getConnection: Connection

  def getUrl: String = {
    connectionUrl
  }

  /**
    * Loads database table to dataframe (using basic jdbc functions)
    * Used in table as a source
    * @param table target table
    * @param username user name
    * @param password password
    * @param sqlContext sql context
    * @return dataframe
    */
  def loadData(
      table: String,
      username: Option[String],
      password: Option[String])(implicit sqlContext: SQLContext): DataFrame = {
    val connectionProperties = new Properties()

    (username, password) match {
      case (Some(u), Some(p)) =>
        connectionProperties.put("user", u)
        connectionProperties.put("password", p)
      case _ =>
    }

    sqlContext.read.jdbc(connectionUrl, table, connectionProperties)
  }

}
