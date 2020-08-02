package de.upb.cs.uc4.test_resources

object TestCourses {

  // Examples
  val courseA: String =
    """{
      |  "courseId": "A",
      |  "courseName": "A course",
      |  "courseType": "Lecture",
      |  "startDate": "2020-06-30",
      |  "endDate": "2020-06-30",
      |  "ects": 10,
      |  "lecturerId": "string",
      |  "maxParticipants": 10,
      |  "currentParticipants": 0,
      |  "courseLanguage": "German",
      |  "courseDescription": "string"
      |}""".stripMargin

  val courseB: String =
    """{
      |  "courseId": "B",
      |  "courseName": "B course",
      |  "courseType": "Lecture",
      |  "startDate": "2020-06-30",
      |  "endDate": "2020-06-30",
      |  "ects": 20,
      |  "lecturerId": "string",
      |  "maxParticipants": 120,
      |  "currentParticipants": 0,
      |  "courseLanguage": "German",
      |  "courseDescription": "string"
      |}""".stripMargin

  val courseInvalid: String =
    """{
      |  "courseId": "B",
      |  "courseName": "B course",
      |  "courseType": "Lecture",
      |  "startDate": "2020-06-30",
      |  "endDate": "2020-06-30",
      |  "ects": 0,
      |  "lecturerId": "string",
      |  "maxParticipants": 120,
      |  "currentParticipants": 0,
      |  "courseLanguage": "German",
      |  "courseDescription": "string"
      |}""".stripMargin
}
