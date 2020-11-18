package de.upb.cs.uc4.examreg

import de.upb.cs.uc4.examreg.model.{ ExaminationRegulation, Module }

trait DefaultTestExamRegs {

  val examReg0: ExaminationRegulation = ExaminationRegulation("Mathematics Bachelor v3", active = true, Seq(
    Module("M.1275.01158", "Math 1"),
    Module("M.1275.56002", "Math 2"),
  ))

  val examReg1: ExaminationRegulation = ExaminationRegulation("Mathematics Bachelor v4", active = true, Seq(
    Module("M.1275.01158", "Math 1"),
    Module("M.1275.56002", "Math 2"),
    Module("M.1275.13337", "Math 3"),
  ))

  val examReg2: ExaminationRegulation = ExaminationRegulation("Mathematics Bachelor v2", active = false, Seq(
    Module("M.1568.98158", "Math 69"),
  ))

}
