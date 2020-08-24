package de.upb.cs.uc4.authentication.test

import de.upb.cs.uc4.authentication.model.{ AuthenticationRole, AuthenticationUser }

trait DefaultTestAuthenticationUsers {

  val authStudent: AuthenticationUser = AuthenticationUser("student", "student", AuthenticationRole.Student)
  val authLecturer: AuthenticationUser = AuthenticationUser("lecturer", "lecturer", AuthenticationRole.Lecturer)
  val authAdmin: AuthenticationUser = AuthenticationUser("admin", "admin", AuthenticationRole.Admin)

}
