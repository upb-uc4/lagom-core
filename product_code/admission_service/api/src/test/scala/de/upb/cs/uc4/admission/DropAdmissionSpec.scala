package de.upb.cs.uc4.admission

import de.upb.cs.uc4.admission.model.DropAdmission
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

class DropAdmissionSpec extends AsyncWordSpecLike with Matchers {

  val dropAdmissionValid: DropAdmission = DropAdmission("exampleAdmissionId")

  "A DropAdmission" should {
    "be validated" in {
      dropAdmissionValid.validateOnCreation.map(_ shouldBe empty)
    }
    "return a validation error with admissionId empty" in {
      dropAdmissionValid.copy(admissionId = "").validateOnCreation.map(_.map(_.name) should contain theSameElementsAs Seq("admissionId"))
    }
  }
}
