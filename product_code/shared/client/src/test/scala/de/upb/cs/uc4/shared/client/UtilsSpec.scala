package de.upb.cs.uc4.shared.client

import de.upb.cs.uc4.shared.client.Utils._
import de.upb.cs.uc4.shared.client.exceptions.SimpleError
import org.scalatest.PrivateMethodTester
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class UtilsSpec extends AnyWordSpecLike with Matchers with PrivateMethodTester {

  "Utils" should {

    "translate semesters into numbers" in {
      val semesterToNumber = PrivateMethod[Double](Symbol("semesterToNumber"))

      Utils invokePrivate semesterToNumber("SS2020") shouldBe 2020.0
      Utils invokePrivate semesterToNumber("WS2021/22") shouldBe 2021.5
      Utils invokePrivate semesterToNumber("") shouldBe Double.MinValue
    }

    "find the latest semester" in {
      val semesterList = Seq("SS2020", "WS2019/20", "SS2020", "WS2020/21", "SS2019")
      findLatestSemester(semesterList) should ===("WS2020/21")
    }
  }

  "SemesterUtils" should {

    "validate correct semester strings" in {
      "SS2020".validateSemester shouldBe empty
      "WS2020/21".validateSemester shouldBe empty
      "WS2099/00".validateSemester shouldBe empty
    }

    "not validate incorrect semester strings" in {
      "SSS020".validateSemester should contain only SimpleError("semester", "Semester must be of the format \"SSyyyy\" for summer, \"WSyyyy/yy\" for winter.")
      "WS202021".validateSemester should contain only SimpleError("semester", "Semester must be of the format \"SSyyyy\" for summer, \"WSyyyy/yy\" for winter.")
      "WS2020/22".validateSemester should contain only SimpleError("semester", "Winter semester must consist of two consecutive years.")
    }

    "compare two semesters in the right way" in {
      "SS2020".compareSemester("WS2020/21") shouldBe -1
      "SS2020".compareSemester("SS2020") shouldBe 0
      "SS2020".compareSemester("SS2019") shouldBe 1
    }
  }
}
