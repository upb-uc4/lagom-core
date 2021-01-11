package de.upb.cs.uc4.matriculation.impl

import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.certificate.CertificateServiceStub
import de.upb.cs.uc4.examreg.{ DefaultTestExamRegs, ExamregServiceStub }
import de.upb.cs.uc4.hyperledger.HyperledgerUtils.JsonUtil.ToJsonUtil
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionMatriculationTrait
import de.upb.cs.uc4.hyperledger.exceptions.traits.TransactionExceptionTrait
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.matriculation.impl.actor.MatriculationBehaviour
import de.upb.cs.uc4.matriculation.model.{ ImmatriculationData, PutMessageMatriculation, SubjectMatriculation }
import de.upb.cs.uc4.shared.client._
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

        userService.resetToDefaults()

        var jsonStringList: Seq[String] = List()

        override def createActorFactory: MatriculationBehaviour = new MatriculationBehaviour(config) {

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
              ("", jSonMatriculationData.getBytes())

            override def getProposalAddEntriesToMatriculationData(certificate: String, AFFILITATION: String = AFFILIATION, enrollmentId: String, subjectMatriculationList: String): (String, Array[Byte]) =
              ("", (enrollmentId + "#" + subjectMatriculationList).getBytes())

            override def getUnsignedTransaction(proposalBytes: Array[Byte], signature: Array[Byte]): Array[Byte] = {
              "t:".getBytes() ++ proposalBytes
            }

            override def submitSignedTransaction(transactionBytes: Array[Byte], signature: Array[Byte]): (String, String) = {
              new String(transactionBytes, StandardCharsets.UTF_8) match {
                case s"t:$enrollmentId#$subjectMatriculationList" =>
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
                  ("", "")

                case s"$jsonMatriculationData" =>
                  val matriculationData = Json.parse(jsonMatriculationData).as[ImmatriculationData]
                  if (matriculationData.matriculationStatus.isEmpty) {
                    throw new TransactionExceptionTrait() {
                      override val transactionName: String = "submitSignedTransaction"
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
                  ("", "")
                case _ =>
                  throw new TransactionExceptionTrait() {
                    override val transactionName: String = "addEntriesToMatriculationData"
                    override val payload: String =
                      """{
                        |  "type": "HLUnprocessableEntity",
                        |  "title": "The following parameters do not conform to the specified format.",
                        |  "invalidParams":[
                        |     {
                        |       "name":"TransactionNotKnown",
                        |       "reason":"Received an unknown transaction!"
                        |     }
                        |  ]
                        |}""".stripMargin
                  }
              }
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

  private def createSingleMatriculation(field: String, semester: String) = PutMessageMatriculation(Seq(SubjectMatriculation(field, Seq(semester))))
  private def createDoubleMatriculation(field: String, semester: String, fieldSecond: String, semesterSecond: String) = PutMessageMatriculation(Seq(SubjectMatriculation(field, Seq(semester)), SubjectMatriculation(fieldSecond, Seq(semesterSecond))))
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
      certificate.getEnrollmentId(student0.username).invoke().flatMap { jsonId =>
        val data = ImmatriculationData(jsonId.id, message.matriculation)

        client.getMatriculationProposal(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username))
          .invoke(message).map { proposal =>
            asString(proposal.unsignedProposal) should ===(data.toJson)
          }.andThen {
            case _ => cleanup()
          }
      }
    }

    "get a transaction for adding matriculation data for a student, given a proposal" in {
      val message = createSingleMatriculation(examReg0.name, "SS2020")
      certificate.setup(student0.username)
      certificate.getEnrollmentId(student0.username).invoke().flatMap { jsonId =>
        val data = ImmatriculationData(jsonId.id, message.matriculation)

        client.getMatriculationProposal(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username))
          .invoke(message).flatMap { proposal =>
            client.submitMatriculationProposal(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username))
              .invoke(SignedProposal(proposal.unsignedProposal, "c2lnbmVk")).flatMap { transaction =>
                asString(transaction.unsignedTransaction) should ===("t:" + data.toJson)
              }
          }.andThen {
            case _ => cleanup()
          }
      }
    }

    "fail getting a proposal for adding matriculation data for a student, given a proposal while using another auth username" in {
      val message = createSingleMatriculation(examReg0.name, "SS2020")
      certificate.setup(student0.username)
      certificate.getEnrollmentId(student0.username).invoke().flatMap { jsonId =>
        client.getMatriculationProposal(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username + "thisShouldFail"))
          .invoke(message).failed.map { answer =>
            answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
          }.andThen {
            case _ => cleanup()
          }
      }
    }

    "fail submitting a proposal for adding matriculation data for a student, given a proposal while using another auth username" in {
      val message = createSingleMatriculation(examReg0.name, "SS2020")
      certificate.setup(student0.username)
      certificate.getEnrollmentId(student0.username).invoke().flatMap { jsonId =>
        client.getMatriculationProposal(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username))
          .invoke(message).flatMap { proposal =>
            client.submitMatriculationProposal(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username + "thisShouldFail"))
              .invoke(SignedProposal(proposal.unsignedProposal, "c2lnbmVk")).failed.map { answer =>
                answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
              }.andThen {
                case _ => cleanup()
              }
          }
      }
    }

    "fail submitting a transaction for adding matriculation data for a student, given a proposal while using another auth username" in {
      certificate.setup(student0.username)
      certificate.getEnrollmentId(student0.username).invoke().flatMap { jsonId =>
        prepare(Seq(
          ImmatriculationData(
            jsonId.id,
            Seq(SubjectMatriculation(examReg0.name, Seq("SS2020")))
          )
        ))
        client.getMatriculationProposal(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username))
          .invoke(createSingleMatriculation(examReg0.name, "WS2020/21")).flatMap {
            proposal =>

              client.submitMatriculationProposal(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username))
                .invoke(SignedProposal(proposal.unsignedProposal, "c2lnbmVk")).flatMap { transaction =>

                  client.submitMatriculationTransaction(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username + "thisShouldFail"))
                    .invoke(SignedTransaction(transaction.unsignedTransaction, "c2lnbmVk")).failed.map(answer =>
                      answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch))
                }
          }.andThen {
            case _ => cleanup()
          }
      }
    }

    "not get  matriculation data for another student" in {
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
        .invoke(createDoubleMatriculation(examReg0.name, "SS2020", "DoesNotExist", "SS2020")).failed.map { answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError]
            .invalidParams.map(_.name) should contain("matriculation[1]")
        }.andThen {
          case _ => cleanup()
        }
    }

    "extend matriculation data of an already existing field of study" in {
      certificate.setup(student0.username)
      certificate.getEnrollmentId(student0.username).invoke().flatMap { jsonId =>
        prepare(Seq(
          ImmatriculationData(
            jsonId.id,
            Seq(SubjectMatriculation(examReg0.name, Seq("SS2020")))
          )
        ))
        client.getMatriculationProposal(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username))
          .invoke(createSingleMatriculation(examReg0.name, "WS2020/21")).flatMap {
            proposal =>

              client.submitMatriculationProposal(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username))
                .invoke(SignedProposal(proposal.unsignedProposal, "c2lnbmVk")).flatMap { transaction =>

                  client.submitMatriculationTransaction(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username))
                    .invoke(SignedTransaction(transaction.unsignedTransaction, "c2lnbmVk")).flatMap { _ =>

                      client.getMatriculationData(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username)).invoke().map {
                        answer =>
                          answer.matriculationStatus should contain theSameElementsAs
                            Seq(SubjectMatriculation(examReg0.name, Seq("SS2020", "WS2020/21")))
                      }
                    }
                }
          }.andThen {
            case _ => cleanup()
          }
      }
    }

    "extend matriculation data with a new field of study" in {
      certificate.setup(student0.username)
      certificate.getEnrollmentId(student0.username).invoke().flatMap { jsonId =>
        prepare(Seq(
          ImmatriculationData(
            jsonId.id,
            Seq(SubjectMatriculation(examReg0.name, Seq("SS2020", "WS2020/21")))
          )
        ))
        client.getMatriculationProposal(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username))
          .invoke(createSingleMatriculation(examReg1.name, "WS2021/22")).flatMap { proposal =>

            client.submitMatriculationProposal(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username))
              .invoke(SignedProposal(proposal.unsignedProposal, "c2lnbmVk")).flatMap { transaction =>

                client.submitMatriculationTransaction(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username))
                  .invoke(SignedTransaction(transaction.unsignedTransaction, "c2lnbmVk")).flatMap { _ =>

                    client.getMatriculationData(student0.username).handleRequestHeader(addAuthorizationHeader(student0.username)).invoke().map {
                      answer =>
                        answer.matriculationStatus should contain theSameElementsAs
                          Seq(
                            SubjectMatriculation(examReg0.name, Seq("SS2020", "WS2020/21")),
                            SubjectMatriculation(examReg1.name, Seq("WS2021/22"))
                          )
                    }
                  }
              }
          }.andThen {
            case _ => cleanup()
          }
      }
    }

    //DEPRECATED
    "use deprecated method that" must {
      "add matriculation data for a student" in {
        certificate.setup(student0.username)
        certificate.getEnrollmentId(student0.username).invoke().flatMap { jsonId =>
          prepare(Seq(
            ImmatriculationData(
              jsonId.id,
              Seq(SubjectMatriculation(examReg0.name, Seq("SS2020")))
            )
          ))
          client.addMatriculationData(student0.username).handleRequestHeader(addAuthorizationHeader())
            .invoke(PutMessageMatriculation(Seq(SubjectMatriculation(examReg0.name, Seq("SS2020"))))).flatMap { _ =>
              client.getMatriculationData(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke().map {
                answer =>
                  answer.enrollmentId should ===(jsonId.id)
                  answer.matriculationStatus should contain theSameElementsAs
                    Seq(SubjectMatriculation(examReg0.name, Seq("SS2020")))
              }
            }.andThen {
              case _ => cleanup()
            }

        }
      }

      "not add empty matriculation data for a student" in {
        certificate.setup(student0.username)
        certificate.getEnrollmentId(student0.username).invoke().flatMap { jsonId =>
          prepare(Seq(
            ImmatriculationData(
              jsonId.id,
              Seq(SubjectMatriculation(examReg0.name, Seq("SS2020", "WS2020/21")))
            )
          ))
          client.addMatriculationData(student0.username).handleRequestHeader(addAuthorizationHeader())
            .invoke(PutMessageMatriculation(Seq())).failed.map { answer =>
              answer.asInstanceOf[UC4Exception].errorCode should ===(422)
            }.andThen {
              case _ => cleanup()
            }
        }
      }

      "extend matriculation data of an already existing field of study" in {
        certificate.setup(student0.username)
        certificate.getEnrollmentId(student0.username).invoke().flatMap { jsonId =>
          prepare(Seq(
            ImmatriculationData(
              jsonId.id,
              Seq(SubjectMatriculation(examReg0.name, Seq("SS2020")))
            )
          ))
          client.addMatriculationData(student0.username).handleRequestHeader(addAuthorizationHeader())
            .invoke(createSingleMatriculation(examReg0.name, "WS2020/21")).flatMap { _ =>
              client.getMatriculationData(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke().map {
                answer =>
                  answer.matriculationStatus should contain theSameElementsAs
                    Seq(SubjectMatriculation(examReg0.name, Seq("SS2020", "WS2020/21")))
              }
            }.andThen {
              case _ => cleanup()
            }
        }
      }

      "extend matriculation data of a non-existing field of study" in {
        certificate.setup(student0.username)
        certificate.getEnrollmentId(student0.username).invoke().flatMap { jsonId =>
          prepare(Seq(
            ImmatriculationData(
              jsonId.id,
              Seq(SubjectMatriculation(examReg0.name, Seq("SS2020", "WS2020/21")))
            )
          ))
          client.addMatriculationData(student0.username).handleRequestHeader(addAuthorizationHeader())
            .invoke(createSingleMatriculation(examReg1.name, "WS2021/22")).flatMap { _ =>
              client.getMatriculationData(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke().map {
                answer =>
                  answer.matriculationStatus should contain theSameElementsAs
                    Seq(
                      SubjectMatriculation(examReg0.name, Seq("SS2020", "WS2020/21")),
                      SubjectMatriculation(examReg1.name, Seq("WS2021/22"))
                    )
              }
            }.andThen {
              case _ => cleanup()
            }
        }
      }
    }

  }
}
