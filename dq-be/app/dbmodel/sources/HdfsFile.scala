package dbmodel.sources

import dbmodel.AppDB
import dbmodel.ModelUtils._
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.annotations.Column

/**
  * Created by Egor Makhov on 28/08/2017.
  */
object HdfsFile {

  // schema > schemaPath
  def applyWithSchema(id: String,
                      keyFields: Seq[String],
                      path: String,
                      fileType: String,
                      separator: Option[String],
                      header: Option[Boolean],
                      schema: Option[List[(String,String)]],
                      schemaPath: Option[String],
                      date: Option[String]): (Source, HdfsFile, List[FileField]) = {
    val kfString: Option[String] = toSeparatedString(keyFields)
    val source = Source(id, "HDFS", kfString)
    if(schema.isDefined){
      val fields = schema.get.map{case (n,v) => FileField(id, n, v)}
      (source, HdfsFile(id, path, fileType, separator, header, None, date),fields)
    } else {
      (source, HdfsFile(id, path, fileType, separator, header, schemaPath, date),List.empty)
    }
  }

  def unapplyWithSchema(bundle: (Source, HdfsFile, List[FileField])): Option[(String, Seq[String], String, String, Option[String], Option[Boolean], Some[List[(String, String)]], Option[String], Option[String])] = {
    val rawSchema = bundle._3.map(ff => (ff.fieldName, ff.fieldType))
    val kfSeq: Seq[String] = parseSeparatedString(bundle._1.keyFields)
    Some(
      bundle._1.id,
      kfSeq,
      bundle._2.path,
      bundle._2.fileType,
      bundle._2.separator,
      bundle._2.header,
      Some(rawSchema),
      bundle._2.schemaPath,
      bundle._2.date)
  }

  def getDetailed(id: String): (Source, HdfsFile) = {
    val src = from(AppDB.sourceTable) (dbt =>
      where(dbt.id === id)
        select dbt
    ).head
    val hdfs = from(AppDB.fileTable) (dbt =>
      where(dbt.id === id)
        select dbt
    ).head
    (src, hdfs)
  }

  def deleteAdditionsById(id: String): Int = {
    AppDB.fileSchemaTable.deleteWhere(ff => ff.owner === id)
  }

  def deleteById(id: String): Int = {
    deleteAdditionsById(id)
    AppDB.fileTable.deleteWhere(db => db.id === id)
    Source.deleteById(id)
  }

  def updateReferencesById(old: String, nuovo: String): Unit = {
    update(AppDB.fileSchemaTable)(ff =>
      where(ff.owner === old)
        set(ff.owner := nuovo)
    )
    Source.updateReferenceById(old, nuovo)
  }

  def fileToMap(source: (Source, HdfsFile)): Map[String, Any] = {
    val kfSeq: Seq[String] = source._1.keyFields match {
      case Some(kf) => kf.split(",").toSeq
      case None => Seq.empty
    }
    val res: Map[String, Option[Any]] = Map(
      "id"-> Option(source._1.id),
      "keyFields" -> Option(kfSeq),
      "path"-> Option(source._2.path),
      "fileType"-> Option(source._2.fileType),
      "separator"-> source._2.separator,
      "header"-> source._2.header,
      "date"-> source._2.date,
      "schema"-> Option(source._2.schema),
      "schemaPath"-> source._2.schemaPath
    )

    res.filter(_._2.isDefined).map(t => t._1 -> t._2.get)
  }
}

case class HdfsFile (
                      id: String,
                      path: String,
                      @Column("file_type")
                      fileType: String,
                      separator: Option[String],
                      header: Option[Boolean],
                      @Column("schema_path")
                      schemaPath: Option[String],
                      date: Option[String]
                    ) extends KeyedEntity[String] {

  def insert(): HdfsFile = {
    AppDB.fileTable.insert(this)
  }
  def update(): HdfsFile = {
    AppDB.fileTable.update(this)
    this
  }
  def rebase(id: String): HdfsFile = {
    this.insert()
    HdfsFile.updateReferencesById(id, this.id)
    HdfsFile.deleteById(id)
    this
  }

  lazy val schema: Iterator[FileField] =
    AppDB.sourceToFields.left(this).iterator
}
