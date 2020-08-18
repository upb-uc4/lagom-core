package de.upb.cs.uc4.matriculation.impl

import de.upb.cs.uc4.matriculation.model.PutMessageMatriculationData
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class PutMessageMatriculationDataSpec extends AnyWordSpecLike with Matchers {
  val putMessageValid: PutMessageMatriculationData = PutMessageMatriculationData("Computer Science", "SS2019")

  "A PutMessageMatriculationData" should {
    "be validated" in {
      val errors = putMessageValid.validate
      errors shouldBe empty
    }
    //FIELD OF STUDY
    "return a validation error for undefined fieldOfStudy" in {
      val errors = putMessageValid.copy(fieldOfStudy = "ThisDoesNot Exist").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("fieldOfStudy")
    }
    //SEMESTER
    "return a validation error for incorrect semester" in {
      val errors = putMessageValid.copy(semester = "FS2020").validate
      val errorVariables = errors.map(error => error.name)
      errorVariables should contain theSameElementsAs Seq("semester")
    }
  }
}
