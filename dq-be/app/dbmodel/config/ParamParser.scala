package dbmodel.config

import com.typesafe.config.{ConfigValue, ConfigValueFactory}

import scala.collection.JavaConversions._
import scala.util.matching.Regex.MatchIterator

/**
  * Created by Egor Makhov on 26/09/2017.
  */
object ParamParser {

  def parseParam(string: String): ConfigValue = {
    val regex = "\\[.*\\]".r

    string match {
      case regex() =>
        val r = """(?:\.,|#)?\w+""".r
        val matches: MatchIterator = r findAllIn string
        ConfigValueFactory.fromIterable(matches.toSeq)
      case _ =>
        ConfigValueFactory.fromAnyRef(string)
    }
  }

  def unquote(string: String): String = {
    val regex = "\".*\"".r
    string match {
      case regex() => string.substring(1, string.length-1)
      case _ => string
    }
  }

}
