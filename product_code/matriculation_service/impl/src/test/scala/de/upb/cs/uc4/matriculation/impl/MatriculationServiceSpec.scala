package de.upb.cs.uc4.matriculation.impl

import java.util.Calendar

import com.lightbend.lagom.scaladsl.api.transport.RequestHeader
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionMatriculationTrait
import de.upb.cs.uc4.hyperledger.exceptions.traits.TransactionExceptionTrait
import de.upb.cs.uc4.matriculation.api.MatriculationService
import de.upb.cs.uc4.matriculation.impl.actor.MatriculationBehaviour
import de.upb.cs.uc4.matriculation.model.{ ImmatriculationData, PutMessageMatriculationData, SubjectMatriculation }
import de.upb.cs.uc4.user.{ DefaultTestUsers, UserServiceStub }
import io.jsonwebtoken.{ Jwts, SignatureAlgorithm }
import org.hyperledger.fabric.gateway.{ Contract, Gateway }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.libs.json.Json
import de.upb.cs.uc4.hyperledger.HyperledgerUtils.JsonUtil.ToJsonUtil

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
          override protected def createConnection: ConnectionMatriculationTrait = new ConnectionMatriculationTrait() {

            override def addMatriculationData(jsonMatriculationData: String): String = {
              jsonStringList :+= jsonMatriculationData
              ""
            }

            override def addEntryToMatriculationData(matriculationId: String, fieldOfStudy: String, semester: String): String = {
              var data = Json.parse(jsonStringList.find(json => json.contains(matriculationId)).get).as[ImmatriculationData]
              val optSubject = data.matriculationStatus.find(_.fieldOfStudy == fieldOfStudy)

              if (optSubject.isDefined) {
                data = data.copy(matriculationStatus = data.matriculationStatus.map { subject =>
                  if (subject != optSubject.get) {
                    subject
                  }
                  else {
                    subject.copy(semesters = subject.semesters :+ semester)
                  }
                })
              }
              else {
                data = data.copy(matriculationStatus = data.matriculationStatus :+ SubjectMatriculation(fieldOfStudy, Seq(semester)))
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
            val contractName: String = ""
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

  "MatriculationService service" should {

    "add matriculation data for a student" in {
      client.addMatriculationData(student0.username).handleRequestHeader(addAuthorizationHeader())
        .invoke(PutMessageMatriculationData("Computer Science", "SS2020")).flatMap { _ =>
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
        .invoke(PutMessageMatriculationData("Computer Science", "WS2020/21")).flatMap { _ =>
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
        .invoke(PutMessageMatriculationData("Mathematics", "WS2020/21")).flatMap { _ =>
          client.getMatriculationData(student0.username).handleRequestHeader(addAuthorizationHeader()).invoke().map {
            answer =>
              answer.matriculationStatus should contain theSameElementsAs
                Seq(
                  SubjectMatriculation("Computer Science", Seq("SS2020", "WS2020/21")),
                  SubjectMatriculation("Mathematics", Seq("WS2020/21"))
                )
          }
        }.andThen {
          case _ => cleanup()
        }
    }
  }
}
