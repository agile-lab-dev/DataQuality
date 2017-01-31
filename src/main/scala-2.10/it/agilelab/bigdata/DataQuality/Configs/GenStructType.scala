package it.agilelab.bigdata.DataQuality.configs

/**
  * Created by Gianvito Siciliano on 12/01/17.
  */

sealed abstract class GenStructField{
  def getType: String
  def name:String
  def tipo:String
}


case class StructField(name:String, tipo:String, format:Option[String] = None) extends GenStructField {
  def getType = "StructField"
}


case class StructFixedField(name:String, tipo:String, length: Int, format:Option[String] = None) extends GenStructField {
  def getType = "StructFixedField"
}