package de.upb.cs.uc4.examreg

import de.upb.cs.uc4.examreg.model.{ ExaminationRegulation, Module }

trait DefaultTestExamRegs {

  val examReg0: ExaminationRegulation = ExaminationRegulation("Computer Science v3", true, Seq(
    Module("M.1275.01158", "Math 1"),
    Module("M.1275.56002", "Math 2"),
    Module("M.1358.91583", "Complexity Theory")
  ))

  val examReg1: ExaminationRegulation = ExaminationRegulation("Computer Science v4", true, Seq(
    Module("M.1568.98158", "Math"),
    Module("M.4186.23588", "Secure Software Engineering")
  ))

  val examReg2: ExaminationRegulation = ExaminationRegulation("Computer Science v4", false, Seq(
    Module("M.1568.98158", "Math"),
    Module("M.4186.23588", "Secure Software Engineering")
  ))

}
