package it.agilelab.bigdata.DataQuality.checks

import scala.util.Try

/**
  * Created by Gianvito Siciliano on 29/12/16.
  */
object CheckUtil {

  /**
    * Takes test object, applies it to the mapping function and map the result to CheckStatus
    * @param tryObject object to try
    * @param successCondition success check function
    * @tparam T type of check object
    * @return CheckStatus (Success/Failure)
    */
  def tryToStatus[T](tryObject: Try[T],
                     successCondition: T => Boolean): CheckStatus =
    tryObject
      .map(
        content => if (successCondition(content)) CheckSuccess else CheckFailure
      )
      .recoverWith {
        // this part is not used actually
        case throwable => Try(CheckError(throwable))
      }
      .get
}
