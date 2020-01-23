package dbmodel.sources

import dbmodel.AppDB
import dbmodel._
import dbmodel.metrics.Metric
import dbmodel.sources.Database.mainTable
import dbmodel.sources.Source.SourceType.SourceType
import org.squeryl.PrimitiveTypeMode. _
import org.squeryl.annotations.Column
import org.squeryl._

/**
  * Created by Egor Makhov on 24/08/2017.
  */

object Source extends SimpleService[Source] {


  protected val mainTable: Table[Source] = AppDB.sourceTable
  protected val virtualTable: Table[VirtualSourceDB] = AppDB.virtualSourceTable

  object SourceType extends Enumeration {
    type SourceType = Value
    val hive = Value("HIVE")
    val table = Value("TABLE")
    val hdfs = Value("HDFS")
    val virtual = Value("VIRTUAL")
  }

  object ParentEnum extends EntityParentEnumeration {
    val database = EntityParentVal("database", getIdsByParentDatabase)

    private def getIdsByParentDatabase(query: String, parentId: String, filter: Option[Enumeration#Value] = None): Query[String] = {
      filter match {
        case _ =>
          from (mainTable, AppDB.dbTableTable)((src, dbt) =>
          where(dbt.database === parentId and src.id === dbt.id and (src.id like query))
            select src.id
          )
      }
    }
  }

  override val typeEnum = Some(SourceType)
  override val parentEnum = Some(ParentEnum)

  override def getIdList(filter: Option[Enumeration#Value] = None): Query[String] = {
    filter match {
      case Some(x: SourceType) => from(mainTable)(tbl => where(tbl.scType === x.toString) select tbl.id)
      case _ => from(mainTable)(tbl => select(tbl.id))
    }
  }

  override def getAll(filter: Option[Enumeration#Value] = None): Query[Source] = {
    filter match {
      case Some(x: SourceType) =>
        from(mainTable)(tbl =>
          where(tbl.scType === x.toString) select tbl)
      case _ => from(mainTable)(tbl => select(tbl))
    }
  }

  def getVirtualFilterBySourceIds(sourceIDs: List[String]): Query[String] = {
    from(mainTable,virtualTable)((tbl,v) =>
      where( v.left in sourceIDs and tbl.id === v.id and v.tipo === "FILTER-SQL"  ) select v.id)

  }

  def getBySourceIDs(sourceIDs: List[String],filter: Option[Enumeration#Value] = None)= {

    filter match {
      case Some(x: SourceType) =>
        from(mainTable)(tbl =>
          where( tbl.id in sourceIDs and tbl.scType === x.toString ) select tbl)
      case _ => from(mainTable)(source=> where( source.id in sourceIDs  ) select(source) )
    }
  }


  override def updateReferenceById(old: String, nuovo: String): Unit = {
    update(AppDB.fileMetricsTable)(met =>
      where(met.source === old)
        set(met.source := nuovo)
    )
    update(AppDB.columnMetricsTable)(met =>
      where(met.source === old)
        set(met.source := nuovo)
    )
  }

  def getSourcesByDatabase(db: String): Query[Source] = {
    from (mainTable, AppDB.dbTableTable)((src, dbt) =>
      where(dbt.database === db and src.id === dbt.id)
        select src
    )
  }

  override def deleteById(id: String): Int = {
    Metric.deleteBySource(id)
    mainTable.deleteWhere(t => t.id === id)
  }

  override def toShortMap(instance: Source): Map[String, Any] = {
    Map("id"-> instance.id, "scType"-> instance.scType)
  }
}

case class Source (
                      id: String,
                      @Column("type")
                      scType: String,
                      @Column("key_fields")
                      keyFields: Option[String] = None
                  )
  extends BasicEntity {
  // keep in mind, that it should be in transaction
  def insert(): Source = {
    AppDB.sourceTable.insert(this)
  }

  def update(): Source = {
    AppDB.sourceTable.update(this)
    this
  }

  def toMap: Map[String, String] = Map("id"-> this.id, "scType"-> this.scType)

  override def getType: String = scType
}
