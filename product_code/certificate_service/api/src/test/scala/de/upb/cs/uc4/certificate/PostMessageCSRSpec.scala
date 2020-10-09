package de.upb.cs.uc4.certificate

import de.upb.cs.uc4.certificate.model.{ EncryptedPrivateKey, PostMessageCSR }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

class PostMessageCSRSpec extends AsyncWordSpecLike with Matchers {

  private val postMessageCSRValid = PostMessageCSR("csrExample", EncryptedPrivateKey("key", "iv", "salt"))
  private val postMessageCSRValidEmpty = PostMessageCSR("csrExample", EncryptedPrivateKey("", "", ""))
  private val postMessageCSRInvalid = PostMessageCSR("csrExample", EncryptedPrivateKey("key", "", "salt"))

  "A PostMessageCSR" should {
    "be validated with empty encrypted private key" in {
      postMessageCSRValidEmpty.validate.map(_ shouldBe empty)
    }

    "be validated with a valid encrypted private key" in {
      postMessageCSRValid.validate.map(_ shouldBe empty)
    }

    "return a validation error for having mixed empty fields in encrypted private key" in {
      postMessageCSRInvalid.validate
        .map(_.map(error => error.name) should contain theSameElementsAs Seq("encryptedPrivateKey"))
    }

  }
}
