package backend

import com.typesafe.config.{Config, ConfigFactory}
import models.config.ConfigReader
import org.scalatestplus.play.PlaySpec

class ConfigParsingSpec extends PlaySpec {

  "Config parser" must {

    val config1: Config = ConfigFactory.parseURL(getClass.getResource("/test1.conf")).resolve()

    "return correct order from config1" in {
      val testOrder: String = "GetYear, YearWrongValues, TipoPrestazione, EstrazioneCodiceGenereDaCF, EstrazioneGenereDaCF, TipoPrestazioneTransform"
      val resOrder: String = ConfigReader.parseVirtualSources(config1).map(_._1).mkString(", ")

      assert(testOrder == resOrder)
    }
  }

}
