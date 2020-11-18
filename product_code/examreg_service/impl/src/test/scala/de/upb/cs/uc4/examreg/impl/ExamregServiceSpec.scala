package de.upb.cs.uc4.examreg.impl

import java.nio.file.Path

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.examreg.DefaultTestExamRegs
import de.upb.cs.uc4.examreg.api.ExamregService
import de.upb.cs.uc4.examreg.impl.actor.ExamregHyperledgerBehaviour
import de.upb.cs.uc4.examreg.model.ExaminationRegulation
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionExaminationRegulationTrait
import de.upb.cs.uc4.shared.client.JsonUtility.{ FromJsonUtil, ToJsonUtil }
import de.upb.cs.uc4.shared.client.exceptions.{ ErrorType, UC4Exception }
import de.upb.cs.uc4.shared.server.UC4SpecUtils
import org.hyperledger.fabric.gateway.impl.{ ContractImpl, GatewayImpl }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.wordspec.AsyncWordSpec

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

            var examRegList: Seq[ExaminationRegulation] = Seq[ExaminationRegulation]()

            override def getProposalAddExaminationRegulation(examinationRegulation: String): Array[Byte] = "getProposalAddExaminationRegulation".getBytes

            override def getProposalGetExaminationRegulations(namesList: String): Array[Byte] = "getProposalGetExaminationRegulations".getBytes

            override def getProposalCloseExaminationRegulation(name: String): Array[Byte] = "getProposalCloseExaminationRegulation".getBytes

            override def addExaminationRegulation(examinationRegulation: String): String = {
              val examReg = examinationRegulation.fromJson[ExaminationRegulation]
              if (examRegList.map(_.name).contains(examReg.name)) {
                throw UC4Exception.Duplicate
              } else {
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

  val defaultExamRegs: Seq[ExaminationRegulation] = server.application.defaultExamRegs

  "ExamregService" should {
    "have a default examination regulation and get names of examination regulations" in {
      eventually(timeout(Span(15, Seconds))) {
        client.getExaminationRegulationsNames(None).invoke().map {
          examRegNames =>
            examRegNames should contain allElementsOf defaultExamRegs.map(_.name)
        }
      }
    }

    "fetch examination regulations" in {
      eventually(timeout(Span(15, Seconds))) {
        client.getExaminationRegulations(Some(defaultExamRegs.head.name), None).invoke().map {
          examRegs =>
            examRegs should contain(defaultExamRegs.head)
        }
      }
    }

    "fetch modules of examination regulations" in {
      eventually(timeout(Span(15, Seconds))) {
        client.getModules(None, None).invoke().map {
          modules =>
            modules should contain allElementsOf defaultExamRegs.flatMap(_.modules)
        }
      }
    }

    "have working query parameters which" must {
      "fetch modules of examination regulations given the moduleIds" in {
        eventually(timeout(Span(15, Seconds))) {
          client.getModules(Some(s"${defaultExamRegs.head.modules.head.id},${defaultExamRegs(1).modules.head.id}"), None).invoke().map {
            modules =>
              modules should contain theSameElementsAs Seq(defaultExamRegs.head.modules.head, defaultExamRegs(1).modules.head)
          }
        }
      }

      "fetch examination regulations given the names" in {
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

    "not add an invalid examination regulation" in {
      client.addExaminationRegulation().handleRequestHeader(addAuthorizationHeader()).invoke(examReg1.copy(modules = Seq()))
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

    "return an error when trying to close a non-existing examination regulation" in {
      client.closeExaminationRegulation("does not exist").handleRequestHeader(addAuthorizationHeader()).invoke().failed.map { exception =>
        exception.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.KeyNotFound)
      }
    }
  }
}
