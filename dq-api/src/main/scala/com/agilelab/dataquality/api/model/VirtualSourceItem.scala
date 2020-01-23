package com.agilelab.dataquality.api.model

import play.api.libs.json._

/**
  * Represents the Swagger definition for SourceItem.
  */
case class VirtualSource(
                          id: String,
                          tipo: String,

                          left: String,
                          right: Option[String] = None,
                          query: String
                        )

object VirtualSourceItem {
  implicit lazy val virtualSourceItemJsonFormat: Format[VirtualSource] = Json.format[VirtualSource]
}

