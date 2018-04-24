package backend

import com.typesafe.config.{Config, ConfigFactory}
import com.agilelab.dataquality.common.models.DatabaseCommon
import com.agilelab.dataquality.common.parsers.DQConfig
import com.agilelab.dataquality.common.parsers.DQConfig.AllErrorsOr
import models.config.ConfigReader
import models.sources.Database
import org.scalatestplus.play.PlaySpec

class ConfigParsingSpec extends PlaySpec {

  "Config parser" must {

    val config1: Config = ConfigFactory.parseURL(getClass.getResource("/test1.conf")).resolve()
    val config2: Config = ConfigFactory.parseURL(getClass.getResource("/test2.conf")).resolve()

    "return correct order from config1" in {
      val testOrder: String = "GetYear, YearWrongValues, TipoPrestazione, EstrazioneCodiceGenereDaCF, EstrazioneGenereDaCF, TipoPrestazioneTransform"
      val resOrder: String = ConfigReader.parseVirtualSources(config1).map(_._1).mkString(", ")

      assert(testOrder == resOrder)
    }

    "parse each object separetely" in {
      import scala.collection.JavaConverters._
      import com.agilelab.dataquality.common.instances.ConfigReaderInstances.databaseReader

      val dbConf = config1.getConfigList("Databases").asScala
      dbConf.map(x => DQConfig.parse[DatabaseCommon](x)).foreach(println)
    }

    "get all databases" in {
      val res: AllErrorsOr[Seq[Database]] = ConfigReader.parseDatabases(config1)
      println(res)
      assert(res.isValid)
    }

    "get errors" in {
      val list: AllErrorsOr[Seq[Database]] = ConfigReader.parseDatabases(config2)
      assert(list.isInvalid)
      list.valueOr(_.toList).foreach(println)
    }
  }

}
