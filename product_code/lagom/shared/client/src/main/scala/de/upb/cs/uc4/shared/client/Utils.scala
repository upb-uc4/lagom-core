package de.upb.cs.uc4.shared.client

import de.upb.cs.uc4.shared.client.exceptions.SimpleError

object Utils {

  implicit class SemesterUtils(val semester: String) {

    /** Validates if a string is in the format of a semester.
     * For example: SS2020, WS2020/21
     *
     * @return a Sequence of [[de.upb.cs.uc4.shared.client.exceptions.SimpleError]]
     */
    def validateSemester: Seq[SimpleError] = {
      var errors: List[SimpleError] = List()

      // Regex for semesters, accepts for example "SS2020" and "WS2020/21"
      val semesterRegex = """(WS[1-9][0-9]{3}/[0-9]{2})|(SS[1-9][0-9]{3})""".r

      if (!semesterRegex.matches(semester)) {
        errors :+= SimpleError("semester", "Semester must be of the format \"SSyyyy\" for summer, \"WSyyyy/yy\" for winter.")
      } else {
        if (semester.substring(0, 2) == "WS" && (semester.substring(4, 6).toInt + 1 != semester.substring(7, 9).toInt)) {
          errors :+= SimpleError("semester", "Winter semester must consist of two consecutive years.")
        }
      }
      errors
    }

    /** Compares two semesters.
     * It is expected that both strings are in the right format validated with validateSemester.
     *
     * @param other is the other semester
     * @return -1 if this semester is before the other
     *         0  if this semester is the same as the other
     *         1  if this semester is after the other
     */
    def compareSemester(other: String): Int = semesterToNumber(semester).compareTo(semesterToNumber(other))
  }

  private def semesterToNumber(semester: String): Double = {
    if (semester.trim.isEmpty) {
      Double.MinValue
    } else {
      if (semester.startsWith("SS")) {
        semester.substring(2).toDouble
      } else {
        semester.substring(2, 6).toDouble + 0.5
      }
    }
  }
}
