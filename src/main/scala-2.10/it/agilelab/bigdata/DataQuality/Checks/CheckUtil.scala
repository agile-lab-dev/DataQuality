package it.agilelab.bigdata.DataQuality.checks

import scala.util.Try


/**
  * Created by Gianvito Siciliano on 29/12/16.
  */

object CheckUtil {

  def tryToStatus[T](tryObject: Try[T], successCondition: T => Boolean): CheckStatus =
    tryObject.map(
      content => if (successCondition(content)) CheckSuccess else CheckFailure
    ).recoverWith {
      case throwable => Try(CheckError(throwable))
    }.get
}
