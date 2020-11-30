package de.upb.cs.uc4.shared.client.configuration

object ErrorMessageCollection {

  object AuthenticationUser {
    val usernameMessage: String = "Username must consist of 4 to 16 characters, and must only contain letters, numbers, '-', and '.'."
    val passwordMessage: String = "Password must not be empty."
  }

  object PostMessageCSR {
    val csrMessage: String = "The certificateSigningRequest must be set."
  }

  object EncryptedPrivateKey {
    val keyMessage: String = "For a non-empty key object, the key must not be longer than 8192 characters."
    val ivMessage: String = "For a non-empty key object, the iv must not be longer than 64 characters."
    val saltMessage: String = "For a non-empty key object, the salt must not be longer than 256 characters."
  }

  object Course {
    val courseNameMessage: String = "Course name must contain between 1 and 100 characters."
    val descriptionMessage: String = "Description must contain 0 to 10000 characters."
    val startDateMessage: String = "Start date must be of the following format \"yyyy-mm-dd\"."
    val endDateMessage: String = "End date must be of the following format \"yyyy-mm-dd\"."
    val ectsMessage: String = "ECTS must be a positive integer between 0 and 999."
    val lecturerNameMessage: String = "LecturerID must not be empty."
    val maxParticipantsMessage: String = "Number of maximum participants must be a positive integer between 1 and 9999."
  }

  object Lecturer {
    val researchAreaMessage: String = "Research area must contain 0 to 200 characters."
  }

  object User {
    val generalMessage: String = "" // Allowed characters for general strings TBD
    val usernameMessage: String = "Username must consist of 4 to 16 characters, and must only contain letters, numbers, '-', and '.'."
    val mailMessage: String = "Email must be in email format example@xyz.com."
    val phoneNumberMessage: String = "Phone number must be of the following format \"+xxxxxxxxxxxx\"."
    val firstNameMessage: String = "First name must contain between 1 and 100 characters."
    val lastNameMessage: String = "Last name must contain between 1 and 100 characters."
  }

  object Address {
    val houseNumberMessage: String = "House number must start with digits and may have trailing letters."
    val streetNameMessage: String = "Street must only contain at letters and '-''."
    val cityNameMessage: String = "City name contains illegal characters."
  }

  object Module {
    val idMessage: String = "ID must contain between 1 and 20 characters."
    val nameMessage: String = "Module name must contain between 1 and 100 characters."
  }

  object ExaminationRegulation {
    val nameMessage: String = "Examination Regulation name must contain between 1 and 100 characters."
  }

  object Commons {
    val dateMessage: String = "Date must be of the following format \"yyyy-mm-dd\"."
    val longTextMessage: String = "Text must contain 0 to 10000 characters."
    val nonEmpty100CharRegex: String = "Field must contain between 1 and 100 characters."
    val nonEmptyCharRegex: String = "Field must contain between 1 and 10000 characters."
  }

}
