package dbmodel.sources

trait SourceItemDAOApi {

  def getAll(page: Option[Int],
             limit: Option[Int],
             sortBy: Option[String],
             orderBy: Option[String]
            )(implicit requestId: String): (Long, List[SourceItemDB])

  def getVirtualAll(implicit requestId: String): List[String]

  def getById(sourceId: String)(implicit requestId: String): SourceItemDB
}
