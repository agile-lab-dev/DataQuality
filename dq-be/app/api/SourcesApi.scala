package api

import com.agilelab.dataquality.api.model._

@javax.annotation.Generated(value = Array("org.openapitools.codegen.languages.ScalaPlayFrameworkServerCodegen"), date = "2019-11-20T10:48:16.257+01:00[Europe/Rome]")
trait SourcesApi {

  /**
    * Retrieve all sources
    * List of sources
    *
    * @param page The current page, with this the backend calculate the offset
    * @param limit The numbers of items to return
    * @param sortBy the field to sort by
    * @param orderBy can be DESC or ASC
    */
  def sourcesList(page: Option[Int], limit: Option[Int],
                  sortBy: Option[String], orderBy: Option[String],
                  onlyVirtual:Boolean=false)(implicit requestId: String): SourcesResultsResponse

  /** Retrieve all virtual sources based on sourceId
    *
    * @param sourceId
    * @param requestId
    * @return
    */
  def getVirtualsourcesBySourceId(sourceId: String)(implicit requestId: String): (List[SourceItem], List[SourceItem])

  /** Retrieve source by id
    *
    * @param sourceId
    * @param requestId
    * @return object [[SourceItem]]
    */
  def sourceById(sourceId: String)(implicit requestId: String): SourceItem

  def virtualSourcesIDGet()(implicit requestId: String): List[String]

}
