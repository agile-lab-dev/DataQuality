package it.agilelab.bigdata.DataQuality.utils.io.db.readers

import java.sql.{Connection, DriverManager, ResultSet}

import it.agilelab.bigdata.DataQuality.sources.DatabaseConfig

/**
  * Created by Egor Makhov on 24/05/2017.
  */
case class PostgresReader(config: DatabaseConfig) extends TableReader {

  override val connectionUrl: String = "jdbc:postgresql://" + config.host

  override def runQuery[T](query: String,
                           transformOutput: ResultSet => T): T = {
    val connection = getConnection

    val statement = connection.createStatement()
    statement.setFetchSize(1000)

    val queryResult = statement.executeQuery(query)
    val result = transformOutput(queryResult)
    statement.close()
    result
  }

  override def getConnection: Connection = {
    val connectionProperties = new java.util.Properties()
    config.user match {
      case Some(user) => connectionProperties.put("user", user)
      case None       =>
    }
    config.password match {
      case Some(pwd) => connectionProperties.put("password", pwd)
      case None      =>
    }
    connectionProperties.put("driver", "org.postgresql.Driver")

    DriverManager.getConnection(connectionUrl, connectionProperties)
  }

}
