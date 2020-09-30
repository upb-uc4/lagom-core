package de.upb.cs.uc4.matriculation

import de.upb.cs.uc4.matriculation.model.{ PutMessageMatriculation, SubjectMatriculation }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

class PutMessageMatriculationSpec extends AsyncWordSpecLike with Matchers {

  val putMessageValid: PutMessageMatriculation = PutMessageMatriculation(Seq(SubjectMatriculation("Computer Science", Seq("SS2019"))))

  val putMessageInvalid: PutMessageMatriculation = PutMessageMatriculation(Seq(
    SubjectMatriculation("Computer Science", Seq("SS2019")),
    SubjectMatriculation("Philosophy", Seq("SS2019", "SW2012"))
  ))

  "A PutMessageMatriculation" should {
    "be validated" in {
      putMessageValid.validate.map(_ shouldBe empty)
    }

    "return a validation error with correct indices" in {
      putMessageInvalid.validate
      .map(_.map(error => error.name) should contain theSameElementsAs Seq("matriculation[1].semesters[1]"))
    }
  }
}
