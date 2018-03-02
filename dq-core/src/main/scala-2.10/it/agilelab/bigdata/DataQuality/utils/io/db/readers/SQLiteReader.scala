package it.agilelab.bigdata.DataQuality.utils.io.db.readers

import java.sql.{Connection, DriverManager, ResultSet}

import it.agilelab.bigdata.DataQuality.sources.DatabaseConfig

/**
  * Created by Egor Makhov on 24/05/2017.
  */
case class SQLiteReader(config: DatabaseConfig) extends TableReader {

  override val connectionUrl: String = "jdbc:sqlite:" + config.host

  override def runQuery[T](query: String,
                           transformOutput: ResultSet => T): T = {
    val metricDBPath: String = "jdbc:sqlite:" + config.host
    val connectionProperties = new java.util.Properties()
    connectionProperties.put("driver", "org.sqlite.JDBC")

    val connection =
      DriverManager.getConnection(metricDBPath, connectionProperties)

    val statement = connection.createStatement()
    statement.setFetchSize(1000)

    val queryResult = statement.executeQuery(query)
    val result = transformOutput(queryResult)
    statement.close()
    result
  }

  override def getConnection: Connection = {
    val connectionProperties = new java.util.Properties()
    connectionProperties.put("driver", "org.sqlite.JDBC")

    DriverManager.getConnection(connectionUrl, connectionProperties)
  }

}
