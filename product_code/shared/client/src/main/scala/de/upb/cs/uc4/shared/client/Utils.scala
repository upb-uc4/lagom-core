package de.upb.cs.uc4.shared.client

import de.upb.cs.uc4.shared.client.exceptions.{ UC4Exception, SimpleError }

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
      }
      else {
        semester match {
          //Check, if in WSYYXX/ZZ it holds, that ZZ = XX+1. No such check for SS necessary
          case s"WS${ x }/${ y }" if (x.toInt + 1) % 100 != y.toInt =>
            errors :+= SimpleError("semester", "Winter semester must consist of two consecutive years.")
          case _ =>
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
    def compareSemester(other: String): Int = {
      if ((semester.validateSemester.nonEmpty && !semester.isEmpty) || other.validateSemester.nonEmpty && !other.isEmpty) {
        throw UC4Exception.InternalServerError("Semester validation error", "The semester string was compared without being validated")
      }
      else {
        semesterToNumber(semester).compareTo(semesterToNumber(other))
      }
    }

  }

  /** Finds the latest semester in a list of semesters
    * @param semesters is a list of valid semesters
    * @return the latest semester
    */
  def findLatestSemester(semesters: Seq[String]): String = {
    if (semesters.forall(_.validateSemester.nonEmpty)) {
      throw UC4Exception.InternalServerError("Semester validation error", "The semester string was used to find latest without being validated")
    }
    semesters.distinct.sortWith((a, b) => a.compareSemester(b) != 1).last
  }

  private def semesterToNumber(semester: String): Double = {
    if (semester.trim.isEmpty) {
      Double.MinValue
    }
    else {
      if (semester.startsWith("SS")) {
        semester.substring(2).toDouble
      }
      else {
        semester.substring(2, 6).toDouble + 0.5
      }
    }
  }
}
