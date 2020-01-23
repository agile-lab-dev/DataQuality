package api

import com.agilelab.dataquality.api.model._

@javax.annotation.Generated(value = Array("org.openapitools.codegen.languages.ScalaPlayFrameworkServerCodegen"), date = "2019-11-20T10:48:16.257+01:00[Europe/Rome]")
trait ChecksApi {
  /**
    * Retrieve a check by id
    * Receive the check result associated
    *
    * @param checkId check id param
    * @param page    The current page, with this the backend calculate the offset
    * @param limit   The numbers of items to return
    * @param sortBy  the field to sort by
    * @param orderBy can be DESC or ASC
    */
  def checkById(checkId: String, page: Option[Int], limit: Option[Int], sortBy: Option[String], orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse

  /**
    * Retrieve a check by id in a time interval
    * Receive the check result associated
    *
    * @param checkId   check id param
    * @param startDate pass a startDate as 2019-11-14T00:00 in iso8601
    * @param endDate   pass a endDate as 2019-11-14T00:00 in iso8601
    * @param page      The current page, with this the backend calculate the offset
    * @param limit     The numbers of items to return
    * @param sortBy    the field to sort by
    * @param orderBy   can be DESC or ASC
    */
  def checkByIdInTimeInterval(checkId: String, startDate: String, endDate: String, page: Option[Int], limit: Option[Int], sortBy: Option[String], orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse

  /**
    * Retrieve all checks
    * Receive all the checks associated
    *
    * @param page    The current page, with this the backend calculate the offset
    * @param limit   The numbers of items to return
    * @param sortBy  the field to sort by
    * @param orderBy can be DESC or ASC
    */
  def getCheckList(page: Option[Int], limit: Option[Int], sortBy: Option[String], orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse

  /**
    * Retrieve all checks for a given metric
    * Passing a metric id you receive all the checks associated
    *
    * @param metricId metric id param
    * @param page     The current page, with this the backend calculate the offset
    * @param limit    The numbers of items to return
    * @param sortBy   the field to sort by
    * @param orderBy  can be DESC or ASC
    */
  def getCheckListByMetricId(metricId: String, page: Option[Int], limit: Option[Int], sortBy: Option[String], orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse

  /**
    * Retrieve all checks for a given source
    * Passing a source id you receive all the checks associated
    *
    * @param sourceId source id param
    * @param page     The current page, with this the backend calculate the offset
    * @param limit    The numbers of items to return
    * @param sortBy   the field to sort by
    * @param orderBy  can be DESC or ASC
    */
  def getCheckListBySourceId(sourceId: String, page: Option[Int], limit: Option[Int], sortBy: Option[String], orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse

  /**
    * Retrieve all checks for a given source
    * Passing a source id you receive all the checks associated
    *
    * @param sourceId  source id param
    * @param startDate pass a startDate as 2019-11-14T00:00 in iso8601
    * @param endDate   pass a endDate as 2019-11-14T00:00 in iso8601
    * @param page      The current page, with this the backend calculate the offset
    * @param limit     The numbers of items to return
    * @param sortBy    the field to sort by
    * @param orderBy   can be DESC or ASC
    */
  def getCheckListBySourceIdInTimeInterval(sourceId: String, startDate: String, endDate: String, page: Option[Int], limit: Option[Int], sortBy: Option[String], orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse

  def getCheckListBySourceIdAndStatusInTimeInterval(sourceId: String, status: String, startDate: String, endDate: String, page: Option[Int], limit: Option[Int], sortBy: Option[String], orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse

  /**
    * Retrieve all checks in a time interval
    * List of checks result in a range of date
    *
    * @param startDate pass a startDate as 2019-11-14T00:00 in iso8601
    * @param endDate   pass a endDate as 2019-11-14T00:00 in iso8601
    * @param page      The current page, with this the backend calculate the offset
    * @param limit     The numbers of items to return
    * @param sortBy    the field to sort by
    * @param orderBy   can be DESC or ASC
    */
  def getCheckListInTimeInterval(startDate: String, endDate: String, page: Option[Int], limit: Option[Int], sortBy: Option[String], orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse

  /**
    * Retrieve all checks by status [Success, Failure]
    * Receive all the checks for a status
    *
    * @param page    The current page, with this the backend calculate the offset
    * @param limit   The numbers of items to return
    * @param sortBy  the field to sort by
    * @param orderBy can be DESC or ASC
    */
  def getCheckListByStatus(status: String, page: Option[Int], limit: Option[Int], sortBy: Option[String], orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse

  /**
    * Retrieve all checks by status [Success, Failure]
    * Receive all the checks for a status
    *
    * @param status    pass String: Success or Failure
    * @param startDate pass a startDate as 2019-11-14T00:00 in iso8601
    * @param endDate   pass a endDate as 2019-11-14T00:00 in iso8601
    * @param page      The current page, with this the backend calculate the offset
    * @param limit     The numbers of items to return
    * @param sortBy    the field to sort by
    * @param orderBy   can be DESC or ASC
    */
  def getCheckListByStatusInTimeInterval(status: String, startDate: String, endDate: String, page: Option[Int], limit: Option[Int], sortBy: Option[String], orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse

  /**
    * Retrieve all checks by tag
    * Receive all the checks fo a given tag
    *
    * @param tagId   tag id param
    * @param page    The current page, with this the backend calculate the offset
    * @param limit   The numbers of items to return
    * @param sortBy  the field to sort by
    * @param orderBy can be DESC or ASC
    */
  def getCheckListByTagId(tagId: String, page: Option[Int], limit: Option[Int], sortBy: Option[String], orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse

  /**
    * Retrieve all checks by tag
    * Receive all the checks fo a given tag
    *
    * @param tagId     tag id param
    * @param startDate pass a startDate as 2019-11-14T00:00 in iso8601
    * @param endDate   pass a endDate as 2019-11-14T00:00 in iso8601
    * @param page      The current page, with this the backend calculate the offset
    * @param limit     The numbers of items to return
    * @param sortBy    the field to sort by
    * @param orderBy   can be DESC or ASC
    */
  def getCheckListByTagIdInTimeInterval(tagId: String, startDate: String, endDate: String, page: Option[Int], limit: Option[Int], sortBy: Option[String], orderBy: Option[String])(implicit requestId: String): ChecksResultsResponse
}
