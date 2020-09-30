package it.agilelab.bigdata.DataQuality.utils

import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest
import it.agilelab.bigdata.DataQuality.sources.DatabaseConfig
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

case class RdsCredentials(
                           engine: String,
                           host: String,
                           port: String,
                           dbName: String,
                           username: String,
                           password: String,
                           connectionUrl: String) {
}


object RdsCredentials {

  def getCredentials(secretName: String): RdsCredentials = {
    val client = AWSSecretsManagerClientBuilder
      .standard()
      .withRegion("eu-central-1")
      .build()
    val request = new GetSecretValueRequest().withSecretId(secretName)

    val response = client.getSecretValue(request)

    implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
    val secret = parse(response.getSecretString)
      .extract[Map[String, String]]
    client.shutdown()


    val engine = secret.getOrElse("engine", "")
    val host = secret.getOrElse("host", "")
    val port = secret.getOrElse("port", "")
    val dbName = secret.getOrElse("dbname", "")
    val username = secret.getOrElse("username", "")
    val password = secret.getOrElse("password", "")
    val connectionUrl: String = s"$host:$port/$dbName"
    RdsCredentials(
      engine,
      host,
      port,
      dbName,
      username,
      password,
      connectionUrl)
  }

  def credentialsToDatabaseConnection(rdsCredentials: RdsCredentials): DatabaseConfig = {
    DatabaseConfig(
      "",
      rdsCredentials.engine.toUpperCase,
      rdsCredentials.connectionUrl,
      Some(rdsCredentials.port),
      None,
      Some(rdsCredentials.username),
      Some(rdsCredentials.password),
      Some("public")
    )
  }
}
