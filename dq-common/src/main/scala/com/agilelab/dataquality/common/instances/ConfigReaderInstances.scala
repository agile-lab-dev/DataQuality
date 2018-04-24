package com.agilelab.dataquality.common.instances

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyList, ValidatedNel}
import cats.implicits._
import com.agilelab.dataquality.common.enumerations.DBTypes
import com.agilelab.dataquality.common.models.DatabaseCommon
import com.agilelab.dataquality.common.parsers.ConfigReader
import com.agilelab.dataquality.common.parsers.DQConfig.AllErrorsOr
import com.typesafe.config.Config

import scala.util.{Failure, Success, Try}

object ConfigReaderInstances {

  private def parseString(str: String)(implicit conf: Config): AllErrorsOr[String] = {
    Try(conf.getString(str)) match {
      case Success(v) => Valid(v)
      case Failure(_) => Invalid(NonEmptyList.one(s"Field $str is missing"))
    }
  }

  private def parseStringEnumerated(str: String, enum: Set[String])(implicit conf: Config): AllErrorsOr[String] = {
    Try(conf.getString(str)) match {
      case Success(v) if enum.contains(v) => Valid(v)
      case Success(v) => Invalid(NonEmptyList.one(s"Unsupported value of $str: $v"))
      case Failure(_) => Invalid(NonEmptyList.one(s"Field $str is missing"))
    }
  }

  implicit val databaseReader: ConfigReader[DatabaseCommon] =
    new ConfigReader[DatabaseCommon] {
      def read(conf: Config, enums: Set[String]*): AllErrorsOr[DatabaseCommon] = {
        val id: AllErrorsOr[String] = parseString("id")(conf)
        val subtype: AllErrorsOr[String] = parseStringEnumerated("subtype", DBTypes.names)(conf)

        Try(conf.getConfig("config")) match {
          case Success(innerConf) =>
            val host: AllErrorsOr[String] = parseString("host")(innerConf)

            val port: AllErrorsOr[Option[Int]] = Valid(Try(innerConf.getString("port").toInt).toOption)
            val service: AllErrorsOr[Option[String]] = Valid(Try(innerConf.getString("service")).toOption)
            val user: AllErrorsOr[Option[String]] = Valid(Try(innerConf.getString("user")).toOption)
            val password: AllErrorsOr[Option[String]] = Valid(Try(innerConf.getString("password")).toOption)
            val schema: AllErrorsOr[Option[String]] = Valid(Try(innerConf.getString("schema")).toOption)

            (id, subtype, host, port, service, user, password, schema).mapN(DatabaseCommon.apply _)
          case Failure(_) => Invalid(NonEmptyList.one("Inner config is missing"))
        }
      }
    }

//  implicit val sourceReader: ConfigReader[SourceCommon] =
//    new ConfigReader[SourceCommon] {
//      override def read(value: Config): AllErrorsOr[SourceCommon] = ???
//    }
}