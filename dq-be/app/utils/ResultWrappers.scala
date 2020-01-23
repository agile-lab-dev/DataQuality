package utils

import dbmodel.MyOwnTypeMode.inTransaction
import play.api.mvc.{Result, Results}

object ResultWrappers extends Results{
  def safeResultInTransaction(f: => Result): Result = {
    inTransaction(try{ f } catch {
      case e: Exception => InternalServerError(e.toString)
    })
  }
}
