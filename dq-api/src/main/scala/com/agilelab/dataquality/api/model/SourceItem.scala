package com.agilelab.dataquality.api.model

import org.squeryl.annotations.Column
import play.api.libs.json._

/**
  * Represents the Swagger definition for SourceItem.
  */
case class SourceItem(
  id: Option[String],
  `type`: Option[String],
  @Column("key_fields")
  keyFields: Option[Array[String]]
){
  def this() = {
    this(None,None,Some(Array.empty))
  }

}

object SourceItem {
  implicit lazy val sourceItemJsonFormat: Format[SourceItem] = Json.format[SourceItem]
}

