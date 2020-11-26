package de.upb.cs.uc4.user

import de.upb.cs.uc4.authentication.model.{ AuthenticationRole, AuthenticationUser }
import de.upb.cs.uc4.user.model.user.{ Admin, Lecturer, Student }
import de.upb.cs.uc4.user.model.{ Address, Role }

trait DefaultTestUsers {

  //USER
  val address0: Address = Address("Gänseweg", "42a", "13337", "Entenhausen", "Germany")
  val address1: Address = Address("Entenweg", "41b", "13342", "Gänsenhausen", "United States")
  val address2: Address = Address("Schwanen Straße", "40c", "13342", "Venice", "Italy")

  val student0: Student = Student("student0", "", isActive = true, Role.Student, address0, "firstName", "LastName", "example0@mail.de", "+49123456789", "1990-12-11", "", "7426915")
  val student1: Student = Student("student1", "", isActive = true, Role.Student, address1, "Aname", "Bname", "exampleD@mail.de", "+49123486688", "1995-04-11", "", "7363186")
  val student2: Student = Student("student2", "", isActive = true, Role.Student, address2, "Volder", "Hahmer", "example2@mail.de", "+49156879773", "2001-02-27", "", "4185966")
  val student0Auth: AuthenticationUser = AuthenticationUser(student0.username, student0.username, AuthenticationRole.Student)
  val student1Auth: AuthenticationUser = AuthenticationUser(student1.username, student1.username, AuthenticationRole.Student)
  val student2Auth: AuthenticationUser = AuthenticationUser(student2.username, student2.username, AuthenticationRole.Student)

  val lecturer0: Lecturer = Lecturer("lecturer0", "", isActive = true, Role.Lecturer, address0, "firstName", "LastName", "example@mail.de", "+49123456789", "1991-12-11", "Heute kommt der kleine Gauss dran.", "Mathematics")
  val lecturer1: Lecturer = Lecturer("lecturer1", "", isActive = true, Role.Lecturer, address1, "Aname", "Bname", "exampleD@mail.de", "+49123486688", "1995-02-13", "Heute kommt nicht der kleine Gauss dran.", "Philosophy")
  val lecturer2: Lecturer = Lecturer("lecturer2", "",isActive = true, Role.Lecturer, address2, "Volder", "Hahmer", "example2@mail.de", "+49156879773", "2001-10-01", "Heute kommt der große Gauss dran.", "Physics")
  val lecturer0Auth: AuthenticationUser = AuthenticationUser(lecturer0.username, lecturer0.username, AuthenticationRole.Lecturer)
  val lecturer1Auth: AuthenticationUser = AuthenticationUser(lecturer1.username, lecturer1.username, AuthenticationRole.Lecturer)
  val lecturer2Auth: AuthenticationUser = AuthenticationUser(lecturer2.username, lecturer2.username, AuthenticationRole.Lecturer)

  val admin0: Admin = Admin("admin0", "", isActive = true, Role.Admin, address0, "firstName", "LastName", "example@mail.de", "+49123456789", "1992-12-11")
  val admin1: Admin = Admin("admin1", "", isActive = true, Role.Admin, address1, "Aname", "Bname", "exampleD@mail.de", "+49123486688", "1995-02-13")
  val admin2: Admin = Admin("admin2", "", isActive = true, Role.Admin, address2, "Volder", "Hahmer", "example2@mail.de", "+49156879773", "2001-10-01")
  val admin0Auth: AuthenticationUser = AuthenticationUser(admin0.username, admin0.username, AuthenticationRole.Admin)
  val admin1Auth: AuthenticationUser = AuthenticationUser(admin1.username, admin1.username, AuthenticationRole.Admin)
  val admin2Auth: AuthenticationUser = AuthenticationUser(admin2.username, admin2.username, AuthenticationRole.Admin)

}
