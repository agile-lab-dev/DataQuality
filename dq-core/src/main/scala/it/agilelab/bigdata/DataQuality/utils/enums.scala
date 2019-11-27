package it.agilelab.bigdata.DataQuality.utils

object enums {

  trait ConfigEnum extends Enumeration {
    def names: Set[String]                    = values.map(_.toString)
    def withNameOpt(s: String): Option[Value] = values.find(_.toString == s)
    def contains(s: String): Boolean          = names.contains(s)
  }

  object Entities extends ConfigEnum {
    val databases: Value = Value("Databases")
    val sources: Value   = Value("Sources")
    val virtual: Value   = Value("VirtualSources")
  }

  object Targets extends ConfigEnum {
    val system: Value = Value("system")
    val hdfs: Value   = Value("hdfs")

    type TargetType = Value
  }
}
