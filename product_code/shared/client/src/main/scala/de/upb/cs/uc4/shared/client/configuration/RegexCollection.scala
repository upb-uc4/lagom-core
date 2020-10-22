package de.upb.cs.uc4.shared.client.configuration

import scala.util.matching.Regex

object RegexCollection {

  object AuthenticationUser {
    val usernameRegex: Regex = """[a-zA-Z0-9-.]{4,16}""".r
    val passwordRegex: Regex = """[\s\S]+""".r
  }

  object PostMessageCSR {
    val csrRegex: Regex = """[\s\S]+""".r
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
    val mailRegex: Regex = """([a-z0-9!#$%&'*+/=?^_`{|}~-]+(\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*)@(([a-z0-9]([a-z0-9-]*[a-z0-9])?\.)+[a-z0-9]([a-z0-9-]*[a-z0-9])?)""".r
    val phoneNumberRegex: Regex = """\+[0-9]{1,30}""".r
  }

  object Address {
    val houseNumberRegex: Regex = """[1-9][0-9]{0,4}([a-zA-Z]([0-9]|-[a-zA-Z])?)?""".r
    val nameRegex: Regex = """[a-zA-Z.,\s\u00c4\u00e4\u00d6\u00f6\u00dc\u00fc\u00df0-9-]{1,50}""".r
  }

  object Course {
    val ectsRegex: Regex = """([1-9][0-9]{0,2})|[0]""".r
    val maxParticipantsRegex: Regex = """[1-9][0-9]{0,3}""".r
  }

  object Commons {
    /** The date regex supports all dates in the format yyyy-mm-dd
      *
      * ((((([0-9]{2})(0[48]|[2468][048]|[13579][26])|(([2468][048]|[13579][26])00))-(02-29))|    // This line handles all 29th of February
      * (([0-9]{4})-                                                                              // This line handles all years
      * (((0[13578]|1[02])-(31))|                                                                 // This line handles all 31st
      * ((0[13-9]|1[0-2])-(29|30))|                                                               // This line handles all 29/30th
      * ((0[1-9])|(1[0-2]))-(0[1-9]|1[0-9]|2[0-8])))))                                            // This line handles all day from 1st to 28th
      */
    val dateRegex: Regex = """((((([0-9]{2})(0[48]|[2468][048]|[13579][26])|(([2468][048]|[13579][26])00))-(02-29))|(([0-9]{4})-(((0[13578]|1[02])-(31))|((0[13-9]|1[0-2])-(29|30))|((0[1-9])|(1[0-2]))-(0[1-9]|1[0-9]|2[0-8])))))""".stripMargin.r

    val longTextRegex: Regex = """[\s\S]{0,10000}""".r
    val nameRegex: Regex = """[\s\S]{1,100}""".r // Allowed characters for name: 1-100 of everything
  }

}
