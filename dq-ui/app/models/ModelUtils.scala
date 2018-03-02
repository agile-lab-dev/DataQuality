package models

/**
  * Created by Egor Makhov on 18/10/2017.
  */
object ModelUtils {

  private lazy val separator: String = ","

  def toSeparatedString(seq: Seq[String]): Option[String] = {
    if (seq.isEmpty) None else Some(seq.mkString(separator))
  }

  def parseSeparatedString(str: Option[String]): Seq[String] = {
    str match {
      case Some(kf) => kf.split(",").toSeq
      case None => Seq.empty
    }
  }

}
