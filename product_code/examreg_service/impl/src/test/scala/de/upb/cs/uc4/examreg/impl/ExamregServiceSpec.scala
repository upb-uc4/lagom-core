package de.upb.cs.uc4.examreg.impl

import java.nio.file.{ Path, Paths }

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.examreg.api.ExamregService
import de.upb.cs.uc4.examreg.impl.actor.ExamregHyperledgerBehaviour
import de.upb.cs.uc4.examreg.model.ExaminationRegulation
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionExaminationRegulationTrait
import de.upb.cs.uc4.shared.client.JsonUtility.FromJsonUtil
import de.upb.cs.uc4.shared.client.JsonUtility.ToJsonUtil
import de.upb.cs.uc4.shared.client.exceptions.UC4Exception
import de.upb.cs.uc4.shared.server.UC4SpecUtils
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class ExamregServiceSpec extends AsyncWordSpec
  with UC4SpecUtils with Matchers with BeforeAndAfterAll {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
      .withJdbc()
  ) { ctx =>
    new ExamregApplication(ctx) with LocalServiceLocator {
      override def createActorFactory: ExamregHyperledgerBehaviour = new ExamregHyperledgerBehaviour(config) {
        override protected def createConnection: ConnectionExaminationRegulationTrait = new ConnectionExaminationRegulationTrait() {

          var examRegList: Seq[ExaminationRegulation] = Seq[ExaminationRegulation]()
          override def getProposalAddExaminationRegulation(examinationRegulation: String): Array[Byte] = "getProposalAddExaminationRegulation".getBytes

          override def getProposalGetExaminationRegulations(namesList: String): Array[Byte] = "getProposalGetExaminationRegulations".getBytes

          override def getProposalCloseExaminationRegulation(name: String): Array[Byte] = "getProposalCloseExaminationRegulation".getBytes

          override def addExaminationRegulation(examinationRegulation: String): String = {
            val examReg = examinationRegulation.fromJson[ExaminationRegulation]
            if (examRegList.map(_.name).contains(examReg.name)){
              throw UC4Exception.Duplicate
            }
            examinationRegulation
          }

          override def getExaminationRegulations(namesList: String): String = {
            examRegList.filter( examReg => namesList.contains(examReg.name))
            examRegList.toJson
          }

          override def closeExaminationRegulation(name: String): String = {
            examRegList.map{
              examReg =>
                if (name == examReg.name){
                  examReg.copy(active = false)
              }
            }
            examRegList.toJson
          }

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

  "ExamregService" should {
    "Have a default examination regulation" in {
      client.getExaminationRegulationsNames(None).invoke().map{
        examRegNames =>
          examRegNames should contain ("Computer Science v3")
      }
    }
  }
}
