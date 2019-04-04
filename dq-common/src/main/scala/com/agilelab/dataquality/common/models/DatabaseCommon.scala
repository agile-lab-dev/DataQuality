package com.agilelab.dataquality.common.models

trait CommonModel

case class DatabaseCommon(
                     id: String,
                     subtype: String,
                     host: String,
                     port: Option[Int],
                     service: Option[String],
                     user: Option[String],
                     password: Option[String],
                     schema: Option[String]
                   ) extends CommonModel

