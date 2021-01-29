package de.upb.cs.uc4.examreg.impl

import java.nio.file.Path

import akka.Done
import akka.actor.ActorSystem
import akka.stream.Materializer
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.examreg.DefaultTestExamRegs
import de.upb.cs.uc4.examreg.api.ExamregService
import de.upb.cs.uc4.examreg.impl.actor.ExamregHyperledgerBehaviour
import de.upb.cs.uc4.examreg.model.{ ExaminationRegulation, Module }
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionExaminationRegulationTrait
import de.upb.cs.uc4.shared.client.JsonHyperledgerVersion
import de.upb.cs.uc4.shared.client.JsonUtility.{ FromJsonUtil, ToJsonUtil }
import de.upb.cs.uc4.shared.client.exceptions.{ ErrorType, UC4Exception }
import de.upb.cs.uc4.shared.server.UC4SpecUtils
import org.hyperledger.fabric.gateway.impl.{ ContractImpl, GatewayImpl }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.wordspec.AsyncWordSpec

import scala.concurrent.{ Await, Future }

class ExamregServiceSpec extends AsyncWordSpec
  with UC4SpecUtils with DefaultTestExamRegs with Matchers with BeforeAndAfterAll with Eventually {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withJdbc()
  ) { ctx =>
      new ExamregApplication(ctx) with LocalServiceLocator {
        override def createActorFactory: ExamregHyperledgerBehaviour = new ExamregHyperledgerBehaviour(config) {

          override val walletPath: Path = retrieveFolderPathWithCreation("uc4.hyperledger.walletPath", "/hyperledger_assets/wallet/")
          override val networkDescriptionPath: Path = retrievePath("uc4.hyperledger.networkConfig", "/hyperledger_assets/connection_profile_kubernetes_local.yaml")
          override val tlsCert: Path = retrievePath("uc4.hyperledger.tlsCert", "")

          override val channel: String = "myc"
          override val chaincode: String = "mycc"
          override val caURL: String = ""

          override val adminUsername: String = "cli"
          override val adminPassword: String = ""

          override protected def createConnection: ConnectionExaminationRegulationTrait = new ConnectionExaminationRegulationTrait() {

            var examRegList: Seq[ExaminationRegulation] = Seq()

            override def getProposalAddExaminationRegulation(certificate: String, affiliation: String = AFFILIATION, examinationRegulation: String): (String, Array[Byte]) = ("", "getProposalAddExaminationRegulation".getBytes)

            override def getProposalGetExaminationRegulations(certificate: String, affiliation: String = AFFILIATION, namesList: String): (String, Array[Byte]) = ("", "getProposalGetExaminationRegulations".getBytes)

            override def getProposalCloseExaminationRegulation(certificate: String, affiliation: String = AFFILIATION, name: String): (String, Array[Byte]) = ("", "getProposalCloseExaminationRegulation".getBytes)

            override def addExaminationRegulation(examinationRegulation: String): String = {
              val examReg = examinationRegulation.fromJson[ExaminationRegulation]
              if (examRegList.map(_.name).contains(examReg.name)) {
                throw UC4Exception.Duplicate
              }
              else {
                examRegList :+= examReg
                examinationRegulation
              }
            }

            override def getExaminationRegulations(namesList: String): String = {
              examRegList.filter(examReg => namesList.contains(examReg.name))
              examRegList.toJson
            }

            override def closeExaminationRegulation(name: String): String = {
              examRegList.map {
                examReg =>
                  if (name == examReg.name) {
                    examReg.copy(active = false)
                  }
              }
              examRegList.toJson
            }

            override def getChaincodeVersion: String = "testVersion"

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

  implicit val system: ActorSystem = server.actorSystem
  implicit val mat: Materializer = server.materializer

  val client: ExamregService = server.serviceClient.implement[ExamregService]

  override protected def afterAll(): Unit = server.stop()

  override protected def beforeAll(): Unit = {
    Future.sequence(tempExamRegs.map { reg =>
      client.addExaminationRegulation().handleRequestHeader(addAuthorizationHeader()).invoke(reg)
    }).map { _ =>
      Future.sequence(defaultExamRegs.map(reg => {
        if (!reg.active) {
          client.closeExaminationRegulation(reg.name).handleRequestHeader(addAuthorizationHeader()).invoke()
        }
        else {
          Future(Done)
        }
      }))
    }
    Await
  }

  val defaultExamRegs: Seq[ExaminationRegulation] = Seq(
    ExaminationRegulation(
      "Bachelor Computer Science v3",
      active = true,
      Seq(
        Module("M.1275.01158", "Math 1"),
        Module("M.1275.01159", "Math 2"),
        Module("M.1275.78235", "Complexity Theory")
      )
    ),
    ExaminationRegulation(
      "Bachelor Computer Science v4",
      active = true,
      Seq(
        Module("M.1278.15686", "Math"),
        Module("M.1275.78235", "Complexity Theory"),
        Module("M.1278.79512", "IT-Security")
      )
    ),
    ExaminationRegulation(
      "Bachelor Philosophy v1",
      active = true,
      Seq(
        Module("M.1358.15686", "Introduction to Philosophy"),
        Module("M.1358.15653", "Theoretical Philosophy"),
        Module("M.1358.15418", "Applied Ethics")
      )
    ),
    ExaminationRegulation(
      "Bachelor Physics",
      active = false,
      Seq(
        Module("M.1358.14686", "Theoretical Physics"),
        Module("M.1358.14653", "Experimental Physics"),
        Module("M.1358.14418", "Physics and Philosophy")
      )
    )
  )
  val tempExamRegs: Seq[ExaminationRegulation] = defaultExamRegs.map(examReg => ExaminationRegulation(examReg.name, active = true, examReg.modules))
  val activeExamRegs: Seq[ExaminationRegulation] = defaultExamRegs.filter(examReg => examReg.active)
  val inactiveExamRegs: Seq[ExaminationRegulation] = defaultExamRegs.filter(examReg => !examReg.active)

  "ExamregService" should {

    //Hyperledger
    "fetch the hyperledger versions" in {
      client.getHlfVersions.invoke().map { answer =>
        answer shouldBe a[JsonHyperledgerVersion]
      }
    }

    //GET
    "get the names of all examination regulations" in {
      eventually(timeout(Span(15, Seconds))) {
        client.getExaminationRegulationsNames(None).invoke().map {
          examRegNames =>
            examRegNames should contain theSameElementsAs defaultExamRegs.map(_.name)
        }
      }
    }

    "get the names of all active examination regulations" in {
      eventually(timeout(Span(15, Seconds))) {
        client.getExaminationRegulationsNames(Some(true)).invoke().map {
          examRegNames =>
            examRegNames should contain theSameElementsAs activeExamRegs.map(_.name)
        }
      }
    }

    "get the names of all inactive examination regulations" in {
      eventually(timeout(Span(15, Seconds))) {
        client.getExaminationRegulationsNames(Some(false)).invoke().map {
          examRegNames =>
            examRegNames should contain theSameElementsAs inactiveExamRegs.map(_.name)
        }
      }
    }

    "get all examination regulations" in {
      eventually(timeout(Span(15, Seconds))) {
        client.getExaminationRegulations(None, None).invoke().map {
          examRegs =>
            examRegs should contain theSameElementsAs defaultExamRegs
        }
      }
    }

    "get all active examination regulations" in {
      eventually(timeout(Span(15, Seconds))) {
        client.getExaminationRegulations(None, Some(true)).invoke().map {
          examRegs =>
            examRegs should contain theSameElementsAs activeExamRegs
        }
      }
    }

    "get all inactive examination regulations" in {
      eventually(timeout(Span(15, Seconds))) {
        client.getExaminationRegulations(None, Some(false)).invoke().map {
          examRegs =>
            examRegs should contain theSameElementsAs inactiveExamRegs
        }
      }
    }

    "get a specific examination regulation" in {
      eventually(timeout(Span(15, Seconds))) {
        client.getExaminationRegulations(Some(defaultExamRegs.head.name), None).invoke().map {
          examRegs =>
            examRegs should contain theSameElementsAs Seq(defaultExamRegs.head)
        }
      }
    }

    "get all modules of all examination regulations" in {
      eventually(timeout(Span(15, Seconds))) {
        client.getModules(None, None).invoke().map {
          modules =>
            modules should contain theSameElementsAs defaultExamRegs.flatMap(_.modules).distinct
        }
      }
    }

    "get only active modules of all examination regulations" in {
      eventually(timeout(Span(15, Seconds))) {
        client.getModules(None, Some(true)).invoke().map {
          modules =>
            modules should contain theSameElementsAs activeExamRegs.flatMap(_.modules).distinct
        }
      }
    }

    "get only inactive modules of all examination regulations" in {
      eventually(timeout(Span(15, Seconds))) {
        client.getModules(None, Some(false)).invoke().map {
          modules =>
            modules should contain theSameElementsAs inactiveExamRegs.flatMap(_.modules)
        }
      }
    }

    "get a specific module" in {
      eventually(timeout(Span(15, Seconds))) {
        client.getModules(Some(defaultExamRegs.head.modules.head.id), None).invoke().map {
          examRegs =>
            examRegs should contain theSameElementsAs Seq(defaultExamRegs.head.modules.head)
        }
      }
    }

    "have working query parameters which" must {
      "get multiple modules of examination regulations given the moduleIds" in {
        eventually(timeout(Span(15, Seconds))) {
          client.getModules(Some(s"${defaultExamRegs.head.modules.head.id},${defaultExamRegs(1).modules.head.id}"), None).invoke().map {
            modules =>
              modules should contain theSameElementsAs Seq(defaultExamRegs.head.modules.head, defaultExamRegs(1).modules.head)
          }
        }
      }
      "fetch multiple examination regulations given the names" in {
        eventually(timeout(Span(15, Seconds))) {
          client.getExaminationRegulations(Some(s"${defaultExamRegs.head.name},${defaultExamRegs(1).name}"), None).invoke().map {
            examRegs =>
              examRegs should contain theSameElementsAs Seq(defaultExamRegs.head, defaultExamRegs(1))
          }
        }
      }
    }

    //POST
    "add an examination regulation" in {
      client.addExaminationRegulation().handleRequestHeader(addAuthorizationHeader()).invoke(examReg0).flatMap { _ =>
        eventually(timeout(Span(15, Seconds))) {
          client.getExaminationRegulations(None, None).invoke().map {
            examRegs =>
              examRegs should contain(examReg0)
          }
        }
      }
    }

    "not add an existing examination regulation" in {
      client.addExaminationRegulation().handleRequestHeader(addAuthorizationHeader()).invoke(examReg0).failed.map {
        exception =>
          {
            // the validation error can only be a SimpleError("name", "An examination regulation with this name does already exist.")
            // because otherwise the test above("add an examination regulation") would fail.
            exception.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.Validation)
          }
      }
    }

    "not add an invalid examination regulation" in {
      client.addExaminationRegulation().handleRequestHeader(addAuthorizationHeader()).invoke(examReg1.copy(modules = Seq()))
        .failed.map { exception =>
          exception.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.Validation)
        }
    }

    "not add an inactive examination regulation" in {
      client.addExaminationRegulation().handleRequestHeader(addAuthorizationHeader()).invoke(examReg1.copy(active = false))
        .failed.map { exception =>
          exception.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.Validation)
        }
    }

    //DELETE
    "close an examination regulation" in {
      client.addExaminationRegulation().handleRequestHeader(addAuthorizationHeader()).invoke(examReg2).flatMap { _ =>
        client.closeExaminationRegulation(examReg2.name).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap {
          _ =>
            eventually(timeout(Span(15, Seconds))) {
              client.getExaminationRegulations(Some(examReg2.name), None).invoke().map {
                examRegs =>
                  examRegs.head should ===(examReg2.copy(active = false))
              }
            }
        }
      }
    }

    "not close an already inactive examination regulation" in {
      val uniqueExamReg = examReg2.copy(name = "inactiveDuplicate")
      client.addExaminationRegulation().handleRequestHeader(addAuthorizationHeader()).invoke(uniqueExamReg).flatMap { _ =>
        client.closeExaminationRegulation(uniqueExamReg.name).handleRequestHeader(addAuthorizationHeader()).invoke().flatMap {
          _ =>
            client.closeExaminationRegulation(uniqueExamReg.name).handleRequestHeader(addAuthorizationHeader()).invoke().failed.map {
              exception =>
                exception.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.AlreadyDeleted)
            }
        }
      }
    }

    "return an error when trying to close a non-existing examination regulation" in {
      client.closeExaminationRegulation("does not exist").handleRequestHeader(addAuthorizationHeader()).invoke().failed.map { exception =>
        exception.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.KeyNotFound)
      }
    }
  }
}
