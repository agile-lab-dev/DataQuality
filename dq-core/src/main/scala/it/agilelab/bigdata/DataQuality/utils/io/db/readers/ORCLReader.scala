package it.agilelab.bigdata.DataQuality.utils.io.db.readers

import java.sql.{Connection, ResultSet}

import it.agilelab.bigdata.DataQuality.sources.DatabaseConfig
import oracle.jdbc.pool.OracleDataSource

/**
  * Created by Egor Makhov on 24/05/2017.
  */
case class ORCLReader(config: DatabaseConfig) extends TableReader {

  override val connectionUrl: String =
    s"jdbc:oracle:thin:@${config.host}:${config.port.get}/${config.service.get}"

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
    val ods = new OracleDataSource()
    ods.setUser(config.user.get)
    ods.setPassword(config.password.get)
    ods.setLoginTimeout(5)
    ods.setURL(connectionUrl)

    ods.getConnection()
  }
}
