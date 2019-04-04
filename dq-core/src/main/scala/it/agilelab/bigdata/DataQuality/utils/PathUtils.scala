package it.agilelab.bigdata.DataQuality.utils

import org.joda.time.DateTime

/**
  * Created by Paolo on 21/01/2017.
  */
object PathUtils extends Logging {

  private val datePlaceholderRegex = """.*(\{\{([^\}]*)\}\}).*"""
  private val datePlaceholderPattern = datePlaceholderRegex.r.pattern

  def replaceDateInPath(path: String, date: DateTime): String = {

    val matches = datePlaceholderPattern.matcher(path)

    if (matches.matches())
      path.replaceAllLiterally(matches.group(1),
                               date.toString(matches.group(2)))
    else
      path

  }

}
