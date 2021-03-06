package de.upb.cs.uc4.matriculation.impl

import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.certificate.CertificateServiceStub
import de.upb.cs.uc4.examreg.{ DefaultTestExamRegs, ExamregServiceStub }
import de.upb.cs.uc4.hyperledger.api.model.operation.{ ApprovalList, OperationData, OperationDataState, TransactionInfo }
import de.upb.cs.uc4.hyperledger.api.model.{ JsonHyperledgerVersion, operation }
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionMatriculationTrait
import de.upb.cs.uc4.hyperledger.exceptions.traits.TransactionExceptionTrait
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.matriculation.impl.actor.MatriculationBehaviour
import de.upb.cs.uc4.matriculation.model.{ ImmatriculationData, PutMessageMatriculation, SubjectMatriculation }
import de.upb.cs.uc4.operation.OperationServiceStub
import de.upb.cs.uc4.shared.client.JsonUtility.ToJsonUtil
import de.upb.cs.uc4.shared.client.exceptions.{ DetailedError, ErrorType, UC4Exception }
import de.upb.cs.uc4.shared.server.UC4SpecUtils
import de.upb.cs.uc4.user.{ DefaultTestUsers, UserServiceStub }
import org.hyperledger.fabric.gateway.impl.{ ContractImpl, GatewayImpl }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.libs.json.Json

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.Base64
import scala.language.reflectiveCalls

/** Tests for the MatriculationService */
class MatriculationServiceSpec extends AsyncWordSpec
  with UC4SpecUtils with Matchers with BeforeAndAfterAll with DefaultTestUsers with DefaultTestExamRegs {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup.withCluster()
  ) { ctx =>
      new MatriculationApplication(ctx) with LocalServiceLocator {
        override lazy val userService: UserServiceStub = new UserServiceStub
        override lazy val certificateService: CertificateServiceStub = new CertificateServiceStub
        override lazy val examregService: ExamregServiceStub = new ExamregServiceStub
        override lazy val operationService: OperationServiceStub = new OperationServiceStub

        userService.resetToDefaults()

        var jsonStringList: Seq[String] = List()

        override def createHyperledgerActor: MatriculationBehaviour = new MatriculationBehaviour(config) {

          override val walletPath: Path = retrieveFolderPathWithCreation("uc4.hyperledger.walletPath", "/hyperledger_assets/wallet/")
          override val networkDescriptionPath: Path = retrievePath("uc4.hyperledger.networkConfig", "/hyperledger_assets/connection_profile_kubernetes_local.yaml")
          override val tlsCert: Path = retrievePath("uc4.hyperledger.tlsCert", "")

          override val channel: String = "myc"
          override val chaincode: String = "mycc"
          override val caURL: String = ""

          override val adminUsername: String = "cli"
          override val adminPassword: String = ""

          override protected def createConnection: ConnectionMatriculationTrait = new ConnectionMatriculationTrait() {

            override def addMatriculationData(jsonMatriculationData: String): String = {
              val matriculationData = Json.parse(jsonMatriculationData).as[ImmatriculationData]
              if (matriculationData.matriculationStatus.isEmpty) {
                throw new TransactionExceptionTrait() {
                  override val transactionName: String = "addEntriesToMatriculationData"
                  override val payload: String =
                    """{
                    |  "type": "HLUnprocessableEntity",
                    |  "title": "The following parameters do not conform to the specified format.",
                    |  "invalidParams":[
                    |     {
                    |       "name":"matriculations",
                    |       "reason":"Matriculation status must not be empty"
                    |     }
                    |  ]
                    |}""".stripMargin
                }
              }
              jsonStringList :+= jsonMatriculationData
              ""
            }

            override def addEntriesToMatriculationData(enrollmentId: String, subjectMatriculationList: String): String = {
              var data = Json.parse(jsonStringList.find(json => json.contains(enrollmentId)).get).as[ImmatriculationData]
              val matriculationList = Json.parse(subjectMatriculationList).as[Seq[SubjectMatriculation]]

              if (matriculationList.isEmpty) {
                throw new TransactionExceptionTrait() {
                  override val transactionName: String = "addEntriesToMatriculationData"
                  override val payload: String =
                    """{
                    |  "type": "HLUnprocessableEntity",
                    |  "title": "The following parameters do not conform to the specified format.",
                    |  "invalidParams":[
                    |     {
                    |       "name":"matriculations",
                    |       "reason":"Matriculation status must not be empty"
                    |     }
                    |  ]
                    |}""".stripMargin
                }
              }

              for (subjectMatriculation: SubjectMatriculation <- matriculationList) {
                val optSubject = data.matriculationStatus.find(_.fieldOfStudy == subjectMatriculation.fieldOfStudy)

                if (optSubject.isDefined) {
                  data = data.copy(matriculationStatus = data.matriculationStatus.map { subject =>
                    if (subject != optSubject.get) {
                      subject
                    }
                    else {
                      subject.copy(semesters = (subject.semesters :++ subjectMatriculation.semesters).distinct)
                    }
                  })
                }
                else {
                  data = data.copy(matriculationStatus = data.matriculationStatus :+ SubjectMatriculation(subjectMatriculation.fieldOfStudy, subjectMatriculation.semesters))
                }
              }

              jsonStringList = jsonStringList.filter(json => !json.contains(enrollmentId)) :+ Json.stringify(Json.toJson(data))
              ""
            }

            override def getMatriculationData(matId: String): String = {
              val mat = jsonStringList.find(json => json.contains(matId))
              if (mat.isDefined) {
                mat.get
              }
              else {
                throw new TransactionExceptionTrait() {
                  override val transactionName: String = "getMatriculationData"
                  override val payload: String =
                    """{
                    |  "type": "HLNotFound",
                    |  "title": "There is no MatriculationData for the given enrollmentId."
                    |}""".stripMargin
                }
              }
            }

            override def getProposalAddMatriculationData(certificate: String, AFFILITATION: String = AFFILIATION, jSonMatriculationData: String): (String, Array[Byte]) =
              (OperationData("mock", TransactionInfo("", "", ""), OperationDataState.PENDING, "", "", "", "", ApprovalList(Seq(), Seq()), ApprovalList(Seq(), Seq())).toJson, jSonMatriculationData.getBytes())

            override def getProposalAddEntriesToMatriculationData(certificate: String, AFFILITATION: String = AFFILIATION, enrollmentId: String, subjectMatriculationList: String): (String, Array[Byte]) =
              (operation.OperationData("mock", TransactionInfo("", "", ""), OperationDataState.PENDING, "", "", "", "", ApprovalList(Seq(), Seq()), ApprovalList(Seq(), Seq())).toJson, (enrollmentId + "#" + subjectMatriculationList).getBytes())

            override def getUnsignedTransaction(proposalBytes: Array[Byte], signature: Array[Byte]): Array[Byte] = {
              "t:".getBytes() ++ proposalBytes
            }

            override def getChaincodeVersion: String = "testVersion"

            override def updateMatriculationData(jSonMatriculationData: String): String = ""

            override def getProposalUpdateMatriculationData(certificate: String, affiliation: String, jSonMatriculationData: String): (String, Array[Byte]) = ("", Array.emptyByteArray)

            override def getProposalGetMatriculationData(certificate: String, affiliation: String, enrollmentId: String): (String, Array[Byte]) = ("", Array.emptyByteArray)

            override val contractName: String = ""
            override lazy val contract: ContractImpl = null
            override lazy val gateway: GatewayImpl = null
            override val username: String = ""
            override val channel: String = ""
            override val chaincode: String = ""
            override val walletPath: Path = null
            override val networkDescriptionPath: Path = null
          }
        }
      }
    }

  val client: MatriculationService = server.serviceClient.implement[MatriculationService]
  val certificate: CertificateServiceStub = server.application.certificateService

  override protected def afterAll(): Unit = server.stop()

  def prepare(matriculations: Seq[ImmatriculationData]): Unit = {
    server.application.jsonStringList ++= matriculations.map(_.toJson)
  }

  def cleanup(): Unit = {
    server.application.jsonStringList = List()
  }

  private def createSingleMatriculation(field: String, semester: String) = createMultiMatriculation((field, semester))
  private def createMultiMatriculation(fieldsAndSemesters: (String, String)*) =
    PutMessageMatriculation(
      fieldsAndSemesters.map {
        case (field, semester) => SubjectMatriculation(field, Seq(semester))
      }
    )
  private def asString(unsignedProposal: String) = new String(Base64.getDecoder.decode(unsignedProposal), StandardCharsets.UTF_8)

  "MatriculationService service" should {

    "fetch the hyperledger versions" in {
      client.getHlfVersions.invoke().map { answer =>
        answer shouldBe a[JsonHyperledgerVersion]
      }
    }

    "get a proposal for adding matriculation data for a student" in {
      val message = createSingleMatriculation(examReg0.name, "SS2020")
      certificate.setup(student0.username)
      certificate.getEnrollmentIds(Some(student0.username)).invoke().flatMap { usernameEnrollmentIdPairSeq =>
        val enrollmentId = usernameEnrollmentIdPairSeq.head.enrollmentId
        val data = ImmatriculationData(enrollmentId, message.matriculation)

        client.getMatriculationProposal(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username))
          .invoke(message).map { proposal =>
            asString(proposal.unsignedProposal) should ===(data.toJson)
          }.andThen {
            case _ => cleanup()
          }
      }
    }

    "fail getting a proposal for adding matriculation data for a student as another student" in {
      val message = createSingleMatriculation(examReg0.name, "SS2020")
      certificate.setup(student0.username)
      certificate.getEnrollmentIds(Some(student0.username)).invoke().flatMap { _ =>
        client.getMatriculationProposal(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username + "thisShouldFail"))
          .invoke(message).failed.map { answer =>
            answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
          }.andThen {
            case _ => cleanup()
          }
      }
    }

    "not get matriculation data for another student" in {
      client.getMatriculationData(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username + "thisShouldFail"))
        .invoke().failed.map { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
        }.andThen {
          case _ => cleanup()
        }
    }

    "not get matriculation data for a lecturer" in {
      client.getMatriculationData(admin0.username).handleRequestHeader(addAuthorizationHeader(admin0.username))
        .invoke().failed.map { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.KeyNotFound)
        }.andThen {
          case _ => cleanup()
        }
    }

    "not add empty matriculation data for a student" in {
      client.getMatriculationProposal(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username))
        .invoke(createSingleMatriculation("", "")).failed.map { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.Validation)
        }.andThen {
          case _ => cleanup()
        }
    }

    "not add matriculation data with non-existing field of study/examreg" in {
      client.getMatriculationProposal(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username))
        .invoke(createSingleMatriculation("DoesNotExist", "SS2020")).failed.map { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError]
            .invalidParams.map(_.name) should contain("matriculation[0]")
        }.andThen {
          case _ => cleanup()
        }
    }

    "not add matriculation data with one existing and one non-existing field of study/examreg" in {
      client.getMatriculationProposal(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username))
        .invoke(createMultiMatriculation((examReg0.name, "SS2020"), ("DoesNotExist", "SS2020"))).failed.map { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError]
            .invalidParams.map(_.name) should contain("matriculation[1]")
        }.andThen {
          case _ => cleanup()
        }
    }
  }
}
