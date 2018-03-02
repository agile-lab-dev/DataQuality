package controllers

/**
  * Created by Egor Makhov on 20/09/2017.
  */
object ControllerUtils {
  // TODO: Add messaging functionality
  lazy val errorUsed: String = "This ID already in use!"
  lazy val errorFormula: String = "This formula is invalid!"
  def errorNotFound(s: String) = s"Provided $s not found!"
  def errorParameterType(name: String, value:String, tipo:String) = s"Parameter '$name'($value) should be '$tipo' type"
}
