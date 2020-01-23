package utils

object UtilFrontend {

  def calculateOffsetLimit(page: Option[Int], limit: Option[Int], total: Long) = {
    (page, limit) match {
      case (Some(pg), Some(lmt)) if pg >= 1 => ((pg - 1) * lmt, lmt)
      case (Some(pg), Some(lmt)) if pg < 1 => (0, lmt)
      case _ => (0, total.toInt)
    }
  }
}
