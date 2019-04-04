package it.agilelab.bigdata.DataQuality.checks.SQLChecks

import java.sql.Connection

import it.agilelab.bigdata.DataQuality.checks.{CheckResult, CheckUtil}
import it.agilelab.bigdata.DataQuality.sources.DatabaseConfig

import scala.util.Try

/**
  * Created by Egor Makhov on 24/05/2017.
  */
/**
  * Performs check based on sql query
  * @param id check id
  * @param description check description
  * @param subType type of check ("EQ_ZERO","NOT_EQ_ZERO",...)
  * @param source database name
  * @param sourceConfig database configuration
  * @param query check query
  * @param date check date
  */
case class SQLCheck(
    id: String,
    description: String,
    subType: String,
    source: String,
    sourceConfig: DatabaseConfig,
    query: String,
    date: String // opt
) {

  def executeCheck(connection: Connection): CheckResult = {
    val transformations = SQLCheckProcessor.getTransformations(subType)

    val statement = connection.createStatement()
    statement.setFetchSize(1000)

    val queryResult = statement.executeQuery(query)

    val result = transformations._1(queryResult)
    statement.close()

    val status = CheckUtil.tryToStatus(Try(result), transformations._2)

    val cr =
      CheckResult(
        this.id,
        subType,
        this.description,
        this.source,
        "",
        Some(""),
        0.0,
        status.stringValue,
        this.query,
        this.date
      )

    cr
  }

}
