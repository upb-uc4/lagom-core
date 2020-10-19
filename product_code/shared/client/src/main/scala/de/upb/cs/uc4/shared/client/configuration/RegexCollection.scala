package de.upb.cs.uc4.shared.client.configuration

import scala.util.matching.Regex

object RegexCollection {

  object AuthenticationUser {
    val usernameRegex: Regex = """[a-zA-Z0-9-.]{4,16}""".r
  }

  object EncryptedPrivateKey {
    val keyRegex: Regex = """[\s\S]{1,8192}""".r
    val ivRegex: Regex = """[\s\S]{1,64}""".r
    val saltRegex: Regex = """[\s\S]{1,256}""".r
  }

  object Lecturer {
    val researchAreaRegex: Regex = """[\s\S]{0,200}""".r
  }

  object User {
    val generalRegex: Regex = """[\s\S]{0,200}""".r // Allowed characters for general strings TBD
    val usernameRegex: Regex = """[a-zA-Z0-9-.]{4,16}""".r
    val mailRegex: Regex = """(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)])""".r
    val phoneNumberRegex: Regex = """\+[0-9]{1,30}""".r
  }

  object Address {
    val houseNumberRegex: Regex = """[1-9][0-9]{0,4}([a-zA-Z]([0-9]|-[a-zA-Z]){0,1}){0,1}""".r
    val nameRegex: Regex = """[a-zA-Z.,\s\u00c4\u00e4\u00d6\u00f6\u00dc\u00fc\u00df0-9-]{1,50}""".r
  }

  object Commons {
    val dateRegex: Regex = """^(?:(?:(?:(?:(?:[1-9]\d)(?:0[48]|[2468][048]|[13579][26])|(?:(?:[2468][048]|[13579][26])00))(-)(?:0?2\1(?:29)))|(?:(?:[1-9]\d{3})(-)(?:(?:(?:0?[13578]|1[02])\2(?:31))|(?:(?:0?[13-9]|1[0-2])\2(?:29|30))|(?:(?:0?[1-9])|(?:1[0-2]))\2(?:0?[1-9]|1\d|2[0-8])))))$""".r
    val longTextRegex: Regex = """[\s\S]{0,10000}""".r
    val nameRegex: Regex = """[\s\S]{1,100}""".r // Allowed characters for name: 1-100 of everything
  }

}
