package de.upb.cs.uc4.matriculation.impl

import java.util.Calendar

import com.lightbend.lagom.scaladsl.api.transport.RequestHeader
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.hyperledger.HyperledgerUtils.JsonUtil.ToJsonUtil
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionMatriculationTrait
import de.upb.cs.uc4.hyperledger.exceptions.traits.TransactionExceptionTrait
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.matriculation.impl.actor.MatriculationBehaviour
import de.upb.cs.uc4.matriculation.model.{ ImmatriculationData, PutMessageMatriculation, SubjectMatriculation }
import de.upb.cs.uc4.shared.client.exceptions.UC4Exception
import de.upb.cs.uc4.user.{ DefaultTestUsers, UserServiceStub }
import io.jsonwebtoken.{ Jwts, SignatureAlgorithm }
import org.hyperledger.fabric.gateway.{ Contract, Gateway }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.libs.json.Json

import scala.language.reflectiveCalls

/** Tests for the MatriculationService
  */
class MatriculationServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll with DefaultTestUsers {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup.withCluster()
  ) { ctx =>
      new MatriculationApplication(ctx) with LocalServiceLocator {
        override lazy val userService: UserServiceStub = new UserServiceStub

        userService.resetToDefaults()

        var jsonStringList: Seq[String] = List()

        override def createActorFactory: MatriculationBehaviour = new MatriculationBehaviour(config) {
          override protected def createConnection(): ConnectionMatriculationTrait = new ConnectionMatriculationTrait() {

            override def addMatriculationData(jsonMatriculationData: String): String = {
              val matriculationData = Json.parse(jsonMatriculationData).as[ImmatriculationData]
              if (matriculationData.matriculationStatus.isEmpty) {
                throw new TransactionExceptionTrait() {
                  override val transactionId: String = "addEntriesToMatriculationData"
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

            override def addEntriesToMatriculationData(matriculationId: String, subjectMatriculationList: String): String = {
              var data = Json.parse(jsonStringList.find(json => json.contains(matriculationId)).get).as[ImmatriculationData]
              val matriculationList = Json.parse(subjectMatriculationList).as[Seq[SubjectMatriculation]]

              if (matriculationList.isEmpty) {
                throw new TransactionExceptionTrait() {
                  override val transactionId: String = "addEntriesToMatriculationData"
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

              jsonStringList = jsonStringList.filter(json => !json.contains(matriculationId)) :+ Json.stringify(Json.toJson(data))
              ""
            }

            override def updateMatriculationData(jSonMatriculationData: String): String = "???"

            override def getMatriculationData(matId: String): String = {
              val mat = jsonStringList.find(json => json.contains(matId))
              if (mat.isDefined) {
                mat.get
              }
              else {
                throw new TransactionExceptionTrait() {
                  override val transactionId: String = "getMatriculationData"
                  override val payload: String =
                    """{
                    |  "type": "HLNotFound",
                    |  "title": "There is no MatriculationData for the given matriculationId."
                    |}""".stripMargin
                }
              }
            }

            override val contract: Contract = null
            override val gateway: Gateway = null
            override val contractName: String = ""
          }
        }
      }
    }

  val client: MatriculationService = server.serviceClient.implement[MatriculationService]

  override protected def afterAll(): Unit = server.stop()

  def addAuthorizationHeader(): RequestHeader => RequestHeader = { header =>
    val time = Calendar.getInstance()
    time.add(Calendar.DATE, 1)

    val token =
      Jwts.builder()
        .setSubject("login")
        .setExpiration(time.getTime)
        .claim("username", "admin")
        .claim("authenticationRole", "Admin")
        .signWith(SignatureAlgorithm.HS256, "changeme")
        .compact()

    header.withHeader("Cookie", s"login=$token")
  }

  def prepare(matriculations: Seq[ImmatriculationData]): Unit = {
    server.application.jsonStringList ++= matriculations.map(_.toJson)
  }

  def cleanup(): Unit = {
    server.application.jsonStringList = List()
  }

  private def createSingleMatriculation(field: String, semester: String) = PutMessageMatriculation(Seq(SubjectMatriculation(field, Seq(semester))))

  "MatriculationService service" should {

    "add matriculation data for a student" in {
      client.addMatriculationData(student0.username).handleRequestHeader(addAuthorizationHeader())
        .invoke(PutMessageMatriculation(Seq(SubjectMatriculation("Computer Science", Seq("SS2020"))))).flatMap { _ =>
          client.getMatriculationData(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke().map {
            answer =>
              answer.matriculationId should ===(student0.matriculationId)
              answer.matriculationStatus should contain theSameElementsAs
                Seq(SubjectMatriculation("Computer Science", Seq("SS2020")))
          }
        }.andThen {
          case _ => cleanup()
        }

    }

    "not add empty matriculation data for a student" in {
      client.addMatriculationData(student0.username).handleRequestHeader(addAuthorizationHeader())
        .invoke(PutMessageMatriculation(Seq())).failed.map { answer =>
          answer.asInstanceOf[UC4Exception].errorCode.http should ===(422)
        }.andThen {
          case _ => cleanup()
        }
    }

    "extend matriculation data of an already existing field of study" in {
      prepare(Seq(
        ImmatriculationData(
          student0.matriculationId,
          student0.firstName,
          student0.lastName,
          student0.birthDate,
          Seq(SubjectMatriculation("Computer Science", Seq("SS2020")))
        )
      ))
      client.addMatriculationData(student0.username).handleRequestHeader(addAuthorizationHeader())
        .invoke(createSingleMatriculation("Computer Science", "WS2020/21")).flatMap { _ =>
          client.getMatriculationData(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke().map {
            answer =>
              answer.matriculationStatus should contain theSameElementsAs
                Seq(SubjectMatriculation("Computer Science", Seq("SS2020", "WS2020/21")))
          }
        }.andThen {
          case _ => cleanup()
        }
    }

    "extend matriculation data of a non-existing field of study" in {
      prepare(Seq(
        ImmatriculationData(
          student0.matriculationId,
          student0.firstName,
          student0.lastName,
          student0.birthDate,
          Seq(SubjectMatriculation("Computer Science", Seq("SS2020", "WS2020/21")))
        )
      ))
      client.addMatriculationData(student0.username).handleRequestHeader(addAuthorizationHeader())
        .invoke(createSingleMatriculation("Mathematics", "WS2021/22")).flatMap { _ =>
          client.getMatriculationData(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke().map {
            answer =>
              answer.matriculationStatus should contain theSameElementsAs
                Seq(
                  SubjectMatriculation("Computer Science", Seq("SS2020", "WS2020/21")),
                  SubjectMatriculation("Mathematics", Seq("WS2021/22"))
                )
          }
        }.andThen {
          case _ => cleanup()
        }
    }
  }
}
