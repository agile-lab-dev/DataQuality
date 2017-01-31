package it.agilelab.bigdata.DataQuality.utils

import org.joda.time.DateTime

/**
  * Created by Paolo on 21/01/2017.
  */
object PathUtils extends Logging{

  val datePlaceholderRegex = """.*(\{\{([^\}]*)\}\}).*"""
  val datePlaceholderPattern = datePlaceholderRegex.r.pattern

  def replaceDateInPath(path: String, date: DateTime): String = {

    val matches = datePlaceholderPattern.matcher(path)
    val formatter = if(matches.matches()){
      (matches.group(1), matches.group(2))
    }else{
      ("not matching","not matching")
    }

    val formattedDate = date.toString(formatter._2)
    val finalPath = path.replaceAllLiterally(formatter._1, formattedDate)

    finalPath

  }

}
