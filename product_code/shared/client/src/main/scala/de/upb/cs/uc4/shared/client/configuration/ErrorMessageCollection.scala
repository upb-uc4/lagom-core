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
    val nameMessage: String = "Course name must contain between 1 and 100 characters."
    val descriptionMessage: String = "Description must contain 0 to 10000 characters."
    val startDateMessage: String = "Start date must be of the following format \"yyyy-mm-dd\"."
    val endDateMessage: String = "End date must be of the following format \"yyyy-mm-dd\"."
    val ectsMessage: String = "ECTS must be a positive integer between 0 and 999."
    val maxParticipantsMessage: String = "Number of maximum participants must be a positive integer between 1 and 9999."
  }

  object Lecturer {
    val researchAreaRegex: String = ""
  }

  object User {
    val generalRegex: String = ""// Allowed characters for general strings TBD
    val usernameRegex: String = ""
    val mailRegex: String = ""
    val phoneNumberRegex: String = ""
  }

  object Address {
    val houseNumberRegex: String = ""
    val nameRegex: String = ""
  }

}
