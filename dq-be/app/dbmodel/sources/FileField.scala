package dbmodel.sources

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import dbmodel.AppDB
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.annotations.Column
import org.squeryl.{KeyedEntity, Query}

/**
  * Created by Egor Makhov on 28/08/2017.
  */
object FileField {
  def getByOwner(id: String): Query[FileField] = {
    from(AppDB.fileSchemaTable) (sc =>
      where(sc.owner === id)
        select sc
    )
  }
  def deleteByName(name: String, owner: String): Int = {
    AppDB.fileSchemaTable.deleteWhere(m => (m.fieldName === name) and (m.owner === owner))
  }
  def deleteByOwner(owner: String): Int = {
    AppDB.fileSchemaTable.deleteWhere(m => m.owner === owner)
  }
}

@JsonIgnoreProperties(Array("owner"))
case class FileField(
                       owner:String,
                       @Column("field_name")
                       fieldName: String,
                       @Column("field_type")
                       fieldType: String
                     ) extends KeyedEntity[(String, String)] {

  override def id: (String, String) = (owner, fieldName)
  def insert(): FileField = AppDB.fileSchemaTable.insert(this)
}
