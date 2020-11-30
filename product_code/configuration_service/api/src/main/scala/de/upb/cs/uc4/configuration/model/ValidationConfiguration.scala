package de.upb.cs.uc4.configuration.model

import de.upb.cs.uc4.configuration.model.ValidationConfiguration._
import de.upb.cs.uc4.shared.client.configuration.{ ErrorMessageCollection, RegexCollection }
import play.api.libs.json.{ Format, Json }

case class ValidationConfiguration(
    authenticationUser: AuthenticationUserRegex,
    postMessageCSR: PostMessageCSRRegex,
    course: CourseRegex,
    user: UserRegex,
    lecturer: LecturerRegex,
    address: AddressRegex,
    examinationRegulation: ExaminationRegulationRegex,
    module: ModuleRegex,
    courseAdmission: CourseAdmissionRegex
)

case class ValidationPair(regex: String, message: String)
object ValidationPair {
  implicit val format: Format[ValidationPair] = Json.format
}

object ValidationConfiguration {
  case class AuthenticationUserRegex(username: ValidationPair, password: ValidationPair)
  object AuthenticationUserRegex {
    implicit val format: Format[AuthenticationUserRegex] = Json.format
  }

  case class PostMessageCSRRegex(certificateSigningRequest: ValidationPair)
  object PostMessageCSRRegex {
    implicit val format: Format[PostMessageCSRRegex] = Json.format
  }

  case class CourseRegex(courseName: ValidationPair, startDate: ValidationPair, endDate: ValidationPair, ects: ValidationPair, lecturerId: ValidationPair, maxParticipants: ValidationPair, courseDescription: ValidationPair)
  object CourseRegex {
    implicit val format: Format[CourseRegex] = Json.format
  }

  case class UserRegex(username: ValidationPair, firstName: ValidationPair, lastName: ValidationPair, email: ValidationPair, phoneNumber: ValidationPair, birthDate: ValidationPair)
  object UserRegex {
    implicit val format: Format[UserRegex] = Json.format
  }

  case class LecturerRegex(freeText: ValidationPair, researchArea: ValidationPair)
  object LecturerRegex {
    implicit val format: Format[LecturerRegex] = Json.format
  }

  case class AddressRegex(street: ValidationPair, houseNumber: ValidationPair, city: ValidationPair)
  object AddressRegex {
    implicit val format: Format[AddressRegex] = Json.format
  }

  case class ExaminationRegulationRegex(name: ValidationPair)
  object ExaminationRegulationRegex {
    implicit val format: Format[ExaminationRegulationRegex] = Json.format
  }

  case class ModuleRegex(id: ValidationPair, name: ValidationPair)
  object ModuleRegex {
    implicit val format: Format[ModuleRegex] = Json.format
  }
  case class CourseAdmissionRegex(enrollmentId: ValidationPair, courseId: ValidationPair, moduleId: ValidationPair)
  object CourseAdmissionRegex {
    implicit val format: Format[CourseAdmissionRegex] = Json.format
  }

  def build: ValidationConfiguration = {
    ValidationConfiguration(
      AuthenticationUserRegex(
        ValidationPair(RegexCollection.AuthenticationUser.usernameRegex.regex, ErrorMessageCollection.AuthenticationUser.usernameMessage),
        ValidationPair(RegexCollection.AuthenticationUser.passwordRegex.regex, ErrorMessageCollection.AuthenticationUser.passwordMessage)
      ),
      PostMessageCSRRegex(
        ValidationPair(RegexCollection.PostMessageCSR.csrRegex.regex, ErrorMessageCollection.PostMessageCSR.csrMessage)
      ),
      CourseRegex(
        ValidationPair(RegexCollection.Commons.nonEmpty100CharRegex.regex, ErrorMessageCollection.Course.courseNameMessage), //courseName
        ValidationPair(RegexCollection.Commons.dateRegex.regex, ErrorMessageCollection.Course.startDateMessage),
        ValidationPair(RegexCollection.Commons.dateRegex.regex, ErrorMessageCollection.Course.endDateMessage),
        ValidationPair(RegexCollection.Course.ectsRegex.regex, ErrorMessageCollection.Course.ectsMessage),
        ValidationPair(RegexCollection.Commons.nonEmpty100CharRegex.regex, ErrorMessageCollection.Course.lecturerNameMessage), //lecturerId
        ValidationPair(RegexCollection.Course.maxParticipantsRegex.regex, ErrorMessageCollection.Course.maxParticipantsMessage),
        ValidationPair(RegexCollection.Commons.longTextRegex.regex, ErrorMessageCollection.Commons.longTextMessage)
      ),
      UserRegex(
        ValidationPair(RegexCollection.User.usernameRegex.regex, ErrorMessageCollection.User.usernameMessage),
        ValidationPair(RegexCollection.Commons.nonEmpty100CharRegex.regex, ErrorMessageCollection.User.firstNameMessage),
        ValidationPair(RegexCollection.Commons.nonEmpty100CharRegex.regex, ErrorMessageCollection.User.lastNameMessage),
        ValidationPair(RegexCollection.User.mailRegex.regex, ErrorMessageCollection.User.mailMessage),
        ValidationPair(RegexCollection.User.phoneNumberRegex.regex, ErrorMessageCollection.User.phoneNumberMessage),
        ValidationPair(RegexCollection.Commons.dateRegex.regex, ErrorMessageCollection.Commons.dateMessage)
      ),
      LecturerRegex(
        ValidationPair(RegexCollection.Commons.longTextRegex.regex, ErrorMessageCollection.Commons.longTextMessage),
        ValidationPair(RegexCollection.Lecturer.researchAreaRegex.regex, ErrorMessageCollection.Lecturer.researchAreaMessage)
      ),
      AddressRegex(
        ValidationPair(RegexCollection.Address.nameRegex.regex, ErrorMessageCollection.Address.streetNameMessage),
        ValidationPair(RegexCollection.Address.houseNumberRegex.regex, ErrorMessageCollection.Address.houseNumberMessage),
        ValidationPair(RegexCollection.Address.nameRegex.regex, ErrorMessageCollection.Address.cityNameMessage)
      ),
      ExaminationRegulationRegex(
        ValidationPair(RegexCollection.Commons.nonEmpty100CharRegex.regex, ErrorMessageCollection.ExaminationRegulation.nameMessage)
      ),
      ModuleRegex(
        ValidationPair(RegexCollection.Module.idRegex.regex, ErrorMessageCollection.Module.idMessage),
        ValidationPair(RegexCollection.Commons.nonEmpty100CharRegex.regex, ErrorMessageCollection.Module.nameMessage)
      ),
      CourseAdmissionRegex(
        ValidationPair(RegexCollection.Commons.nonEmptyCharRegex.regex, ErrorMessageCollection.Commons.nonEmptyCharRegex),
        ValidationPair(RegexCollection.Commons.nonEmptyCharRegex.regex, ErrorMessageCollection.Commons.nonEmptyCharRegex),
        ValidationPair(RegexCollection.Commons.nonEmptyCharRegex.regex, ErrorMessageCollection.Commons.nonEmptyCharRegex)
      )
    )

  }
  implicit val format: Format[ValidationConfiguration] = Json.format
}