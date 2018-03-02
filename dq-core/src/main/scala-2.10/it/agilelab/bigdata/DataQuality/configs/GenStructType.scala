package it.agilelab.bigdata.DataQuality.configs

/**
  * Created by Gianvito Siciliano on 12/01/17.
  *
  * Representation of columns for schema parsing
  */
sealed abstract class GenStructColumn {
  def getType: String
  def name: String
  def tipo: String
}

case class StructColumn(name: String,
                        tipo: String,
                        format: Option[String] = None)
    extends GenStructColumn {
  def getType = "StructColumn"
}

case class StructFixedColumn(name: String,
                             tipo: String,
                             length: Int,
                             format: Option[String] = None)
    extends GenStructColumn {
  def getType = "StructFixedColumn"
}
