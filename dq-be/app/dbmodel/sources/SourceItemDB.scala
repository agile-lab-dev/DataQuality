package dbmodel.sources

import org.squeryl.annotations.Column

/**
  * Represents the Swagger definition for SourceItem.
  */
case class SourceItemDB(
    id: Option[String],
    `type`: Option[String],
    @Column("key_fields")
    keyFields: Option[Array[String]]
) {
  def this() = {
    this(None, None, Some(Array.empty))
  }

}
