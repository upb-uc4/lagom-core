import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.{ ProducerStub, ProducerStubFactory, ServiceTest, TestTopicComponents }
import de.upb.cs.uc4.certificate.CertificateServiceStub
import de.upb.cs.uc4.certificate.api.CertificateService
import de.upb.cs.uc4.certificate.model.EnrollmentUser
import de.upb.cs.uc4.group.api.GroupService
import de.upb.cs.uc4.group.impl.GroupApplication
import de.upb.cs.uc4.group.impl.actor.GroupBehaviour
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionGroupTrait
import de.upb.cs.uc4.shared.client.JsonHyperledgerVersion
import de.upb.cs.uc4.shared.client.kafka.EncryptionContainer
import de.upb.cs.uc4.shared.server.UC4SpecUtils
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.wordspec.AsyncWordSpec

import java.nio.file.Path
import scala.collection.mutable
import scala.concurrent.Future

class GroupServiceSpec extends AsyncWordSpec
  with UC4SpecUtils with Matchers with BeforeAndAfterAll with Eventually {

  private var registrationStub: ProducerStub[EncryptionContainer] = _ //EncryptionContainer[Usernames]
  private val groupMap: mutable.HashMap[String, Seq[String]] = mutable.HashMap()

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withCluster()
  ) { ctx =>
      new GroupApplication(ctx) with LocalServiceLocator with TestTopicComponents {

        lazy val stubFactory = new ProducerStubFactory(actorSystem, materializer)
        lazy val internRegistrationStub: ProducerStub[EncryptionContainer] =
          stubFactory.producer[EncryptionContainer](CertificateService.REGISTRATION_TOPIC_NAME)
        registrationStub = internRegistrationStub

        // Create a userService with ProducerStub as topic
        override lazy val certificateService: CertificateServiceStubWithTopic = new CertificateServiceStubWithTopic(internRegistrationStub)

        override def createHyperledgerActor: GroupBehaviour = new GroupBehaviour(config) {

          override val walletPath: Path = retrieveFolderPathWithCreation("uc4.hyperledger.walletPath", "/hyperledger_assets/wallet/")
          override val networkDescriptionPath: Path = retrievePath("uc4.hyperledger.networkConfig", "/hyperledger_assets/connection_profile_kubernetes_local.yaml")
          override val tlsCert: Path = retrievePath("uc4.hyperledger.tlsCert", "")

          override val channel: String = "myc"
          override val chaincode: String = "mycc"
          override val caURL: String = ""

          override val adminUsername: String = "cli"
          override val adminPassword: String = ""

          override protected def createConnection: ConnectionGroupTrait = new ConnectionGroupTrait {
            override def getProposalAddUserToGroup(certificate: String, affiliation: String, enrollmentId: String, groupId: String): (String, Array[Byte]) = ("", Array.emptyByteArray)

            override def getProposalRemoveUserFromGroup(certificate: String, affiliation: String, enrollmentId: String, groupId: String): (String, Array[Byte]) = ("", Array.emptyByteArray)

            override def getProposalRemoveUserFromAllGroups(certificate: String, affiliation: String, enrollmentId: String): (String, Array[Byte]) = ("", Array.emptyByteArray)

            override def getProposalGetAllGroups(certificate: String, affiliation: String): (String, Array[Byte]) = ("", Array.emptyByteArray)

            override def getProposalGetUsersForGroup(certificate: String, affiliation: String, groupId: String): (String, Array[Byte]) = ("", Array.emptyByteArray)

            override def getProposalGetGroupsForUser(certificate: String, affiliation: String, enrollmentId: String): (String, Array[Byte]) = ("", Array.emptyByteArray)

            override def addUserToGroup(enrollmentId: String, groupId: String): String = {
              groupMap.put(enrollmentId, groupMap.getOrElse(enrollmentId, Seq()).appended(groupId))
              ""
            }

            override def removeUserFromGroup(enrollmentId: String, groupId: String): String = ""

            override def removeUserFromAllGroups(enrollmentId: String): String = ""

            override def getAllGroups: String = ""

            override def getUsersForGroup(groupId: String): String = ""

            override def getGroupsForUser(enrollmentId: String): String = ""

            override def getChaincodeVersion: String = "testVersion"

            override val username: String = ""
            override val channel: String = ""
            override val chaincode: String = ""
            override val walletPath: Path = null
            override val networkDescriptionPath: Path = null
          }
        }
      }
    }

  val client: GroupService = server.serviceClient.implement[GroupService]

  "The GroupService" should {

    "fetch the hyperledger versions" in {
      client.getHlfVersions.invoke().map { answer =>
        answer shouldBe a[JsonHyperledgerVersion]
      }
    }

    "add a user to a group" in {
      val enrollmentId = "student00"
      val container = server.application.kafkaEncryptionUtility.encrypt(EnrollmentUser(enrollmentId, "Student"))
      registrationStub.send(container)

      eventually(timeout(Span(30, Seconds))) {
        Future(groupMap(enrollmentId) should contain theSameElementsAs Seq("Student"))
      }
    }
  }

}

class CertificateServiceStubWithTopic(registrationStub: ProducerStub[EncryptionContainer]) extends CertificateServiceStub {

  override def userEnrollmentTopic(): Topic[EncryptionContainer] = registrationStub.topic
}
