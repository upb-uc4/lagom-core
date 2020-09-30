package de.upb.cs.uc4.matriculation

import de.upb.cs.uc4.matriculation.model.SubjectMatriculation
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

class SubjectMatriculationSpec extends AsyncWordSpecLike with Matchers {

  val subjectMatriculationValid: SubjectMatriculation = SubjectMatriculation("Computer Science", Seq("SS2019"))

  "A SubjectMatriculation" should {
    "be validated" in {
      subjectMatriculationValid.validate.map(_ shouldBe empty)
    }

    //FIELD OF STUDY
    "return a validation error for undefined fieldOfStudy" in {
      subjectMatriculationValid.copy(fieldOfStudy = "ThisDoesNot Exist").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("fieldOfStudy"))
    }

    //SEMESTER
    "return a validation error for incorrect semester" in {
      subjectMatriculationValid.copy(semesters = Seq("SS2019", "FS2020")).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("semesters[1]"))
    }
  }
}
