package backend

import com.agilelab.dataquality.common.enumerations.DBTypes
import org.scalatestplus.play.PlaySpec

class UtilSpec extends PlaySpec {

  "ui" must {
    "have access to other module code" in {
      assert(DBTypes.names == Set("ORACLE", "POSTGRES", "SQLITE"))
    }
  }

  "test" must{
    "toast" in {
      def f(i: Int, enums: Set[Any]*): Set[Any] = {
        if (enums.nonEmpty && i >= 0 && i < enums.size) enums(i)
        else Set("huj")
      }

      println(
        f(1, Set(1,2,3), Set(2,3,4))
      )
    }
  }

}
