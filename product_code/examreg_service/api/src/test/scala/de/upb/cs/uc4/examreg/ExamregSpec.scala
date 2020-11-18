package de.upb.cs.uc4.examreg

import de.upb.cs.uc4.examreg.model.Module
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

class ExamregSpec extends AsyncWordSpecLike with DefaultTestExamRegs with Matchers {

  val moduleValid: Module = Module("M.123.45678", "Test Module")

  "An examination regulation" should {
    "validate" in {
      examReg0.validate.map(_ shouldBe empty)
    }

    "return a validation error for name with wrong length" in {
      examReg0.copy(name = "").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("name"))
    }

    "return a validation error if active is set to false" in {
      examReg0.copy(active = false).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("active"))
    }

    "return a validation error if the module list is empty" in {
      examReg0.copy(modules = Seq()).validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("modules"))
    }

    "return a validation error if one of the modules is incorrect" in {
      val incorrectModules = examReg0.modules.map(_.copy(id = ""))
      examReg0.copy(modules = incorrectModules).validate
        .map(_.map(_.name) should contain theSameElementsAs Seq("modules[0].id","modules[1].id"))
    }
  }

  "A module" should {
    "validate" in {
      moduleValid.validate.map(_ shouldBe empty)
    }

    "return a validation error for id with wrong length" in {
      moduleValid.copy("").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("id"))
    }

    "return a validation error for name with wrong length" in {
      moduleValid.copy(name = "").validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("name"))
    }

  }
}
