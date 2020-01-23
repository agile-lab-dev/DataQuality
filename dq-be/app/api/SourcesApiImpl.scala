package api

import com.agilelab.dataquality.api.model._
import com.google.inject.Inject
import dbmodel.sources.{SourceItemDAO, SourceItemDB}
import dbmodel.sources.VirtualSourceDB


/**
  * Provides a default implementation for [[SourcesApi]].
  */
class SourcesApiImpl  @Inject()(dao:SourceItemDAO) extends SourcesApi {

  /**
    * @inheritdoc
    */
  override def sourcesList(page: Option[Int],
                           limit: Option[Int],
                           sortBy: Option[String],
                           orderBy: Option[String],
                           onlyVirtual: Boolean= false)(implicit requestId: String): SourcesResultsResponse = {

    val r: (Long, List[SourceItemDB]) = dao getAll (page, limit,sortBy, orderBy)
    SourcesResultsResponse(Some(r._1.toInt), Some(r._2.map(s => dBModelToApiModel(s))))

  }

  override def virtualSourcesIDGet()(implicit requestId: String): List[String] = {
     dao getVirtualAll
  }

  private def dBModelToApiModel(s: SourceItemDB): SourceItem = {
    SourceItem(s.id, s.`type`, s.keyFields)
  }

  private def dBModelToApiModel(s: VirtualSourceDB): SourceItem = {
    SourceItem(Some(s.id), Some("VIRTUAL"), None)
  }

  /**
    * @inheritdoc
    */
  override def sourceById(sourceId: String)(implicit requestId: String): SourceItem = {

    val r = dao getById sourceId
    dBModelToApiModel(r)

  }

  override def getVirtualsourcesBySourceId(sourceId: String)(implicit requestId: String): (List[SourceItem], List[SourceItem]) = {

    val r: (List[SourceItemDB], List[VirtualSourceDB]) = dao getTreeById sourceId

    ( r._1.map(s=>dBModelToApiModel(s)), r._2.map(s => dBModelToApiModel(s)) )
  }
}
