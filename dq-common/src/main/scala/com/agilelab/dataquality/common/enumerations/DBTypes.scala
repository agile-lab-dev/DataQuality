package com.agilelab.dataquality.common.enumerations

trait ConfigEnum extends Enumeration{
  def names: Set[String] = values.map(_.toString)
  def withNameOpt(s: String): Option[Value] = values.find(_.toString == s)
  def contains(s: String): Boolean = names.contains(s)
}

object DBTypes extends ConfigEnum {
  val postgres: Value = Value("POSTGRES")
  val oracle: Value = Value("ORACLE")
  val sqlite: Value = Value("SQLITE")
}

object CollectionNames extends ConfigEnum {
  val databases: Value = Value("Databases")
  val sources: Value = Value("Sources")
  val virtual: Value = Value("VirtualSources")
}


