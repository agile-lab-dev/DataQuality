package it.agilelab.bigdata.DataQuality.utils.mailing

import com.typesafe.config.Config

import scala.util.Try

case class MailerConfiguration(
                                address: String,
                                hostName: String,
                                username: String,
                                password: String,
                                smtpPortSSL: Int,
                                sslOnConnect: Boolean
                              ) {
  def this(config: Config) = {
    this(
      config.getString("address"),
      config.getString("hostname"),
      Try(config.getString("username")).getOrElse(""),
      Try(config.getString("password")).getOrElse(""),
      Try(config.getInt("smtpPort")).getOrElse(465),
      Try(config.getBoolean("sslOnConnect")).getOrElse(true)
    )
  }
}
