package de.upb.cs.uc4.admission.impl

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.Base64

import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.admission.api.AdmissionService
import de.upb.cs.uc4.admission.impl.actor.AdmissionBehaviour
import de.upb.cs.uc4.admission.model.{ AdmissionType, CourseAdmission, DropAdmission, ExamAdmission }
import de.upb.cs.uc4.certificate.CertificateServiceStub
import de.upb.cs.uc4.course.{ CourseServiceStub, DefaultTestCourses }
import de.upb.cs.uc4.exam.model.Exam
import de.upb.cs.uc4.exam.{ DefaultTestExams, ExamServiceStub }
import de.upb.cs.uc4.examreg.{ DefaultTestExamRegs, ExamregServiceStub }
import de.upb.cs.uc4.hyperledger.api.model
import de.upb.cs.uc4.hyperledger.api.model.JsonHyperledgerVersion
import de.upb.cs.uc4.hyperledger.api.model.operation.{ ApprovalList, OperationData, OperationDataState, TransactionInfo }
import de.upb.cs.uc4.hyperledger.connections.traits.ConnectionAdmissionTrait
import de.upb.cs.uc4.matriculation.MatriculationServiceStub
import de.upb.cs.uc4.operation.OperationServiceStub
import de.upb.cs.uc4.shared.client.JsonUtility._
import de.upb.cs.uc4.shared.client.exceptions.{ DetailedError, ErrorType, SimpleError, UC4Exception }
import de.upb.cs.uc4.shared.server.UC4SpecUtils
import de.upb.cs.uc4.user.DefaultTestUsers
import org.hyperledger.fabric.gateway.impl.{ ContractImpl, GatewayImpl }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }
import play.api.libs.json.Json

import scala.language.reflectiveCalls

/** Tests for the AdmissionService */
class AdmissionServiceSpec extends AsyncWordSpec
  with UC4SpecUtils with Matchers with BeforeAndAfterAll with DefaultTestUsers with DefaultTestCourses with DefaultTestExams with DefaultTestExamRegs with BeforeAndAfterEach {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup.withCluster()
  ) { ctx =>
      new AdmissionApplication(ctx) with LocalServiceLocator {
        override lazy val certificateService: CertificateServiceStub = new CertificateServiceStub
        override lazy val matriculationService: MatriculationServiceStub = new MatriculationServiceStub
        override lazy val examRegService: ExamregServiceStub = new ExamregServiceStub
        override lazy val examService: ExamServiceStub = new ExamServiceStub
        override lazy val courseService: CourseServiceStub = new CourseServiceStub
        override lazy val operationService: OperationServiceStub = new OperationServiceStub

        certificateService.setup(student0.username, student1.username, student2.username, lecturer0.username)
        examService.setup()

        matriculationService.addImmatriculationData(student0.username, matriculationService.createSingleImmatriculationData(
          certificateService.get(student0.username).enrollmentId, examReg0.name, "SS2020"
        ))
        matriculationService.addImmatriculationData(student1.username, matriculationService.createSingleImmatriculationData(
          certificateService.get(student1.username).enrollmentId, examReg1.name, "SS2020"
        ))
        matriculationService.addImmatriculationData(student2.username, matriculationService.createSingleImmatriculationData(
          certificateService.get(student2.username).enrollmentId, examReg2.name, "SS2020"
        ))

        var courseAdmissionList: Seq[CourseAdmission] = Seq()
        var examAdmissionList: Seq[ExamAdmission] = Seq()

        override def createHyperledgerActor: AdmissionBehaviour = new AdmissionBehaviour(config) {

          override val walletPath: Path = retrieveFolderPathWithCreation("uc4.hyperledger.walletPath", "/hyperledger_assets/wallet/")
          override val networkDescriptionPath: Path = retrievePath("uc4.hyperledger.networkConfig", "/hyperledger_assets/connection_profile_kubernetes_local.yaml")
          override val tlsCert: Path = retrievePath("uc4.hyperledger.tlsCert", "")

          override val channel: String = "myc"
          override val chaincode: String = "mycc"
          override val caURL: String = ""

          override val adminUsername: String = "cli"
          override val adminPassword: String = ""

          override protected def createConnection: ConnectionAdmissionTrait = new ConnectionAdmissionTrait() {

            override def getProposalGetAdmissions(certificate: String, affiliation: String = AFFILIATION, enrollmentId: String = "", courseId: String = "", moduleId: String = ""): (String, Array[Byte]) = ("", Array.empty)

            override def getCourseAdmissions(enrollmentId: String, courseId: String, moduleId: String): String = {
              Json.stringify(Json.toJson(
                courseAdmissionList
                  .filter(admission => enrollmentId == "" || admission.enrollmentId == enrollmentId)
                  .filter(admission => courseId == "" || admission.courseId == courseId)
                  .filter(admission => moduleId == "" || admission.moduleId == moduleId)
              ))
            }

            override def getExamAdmissions(admissionIds: Seq[String], enrollmentId: String, examIds: Seq[String]): String = {
              Json.stringify(Json.toJson(
                examAdmissionList
                  .filter(admission => enrollmentId == "" || admission.enrollmentId == enrollmentId)
                  .filter(admission => examIds.isEmpty || examIds.contains(admission.examId))
                  .filter(admission => admissionIds.isEmpty || admissionIds.contains((admission.admissionId)))
              ))
            }

            override def getProposalAddAdmission(certificate: String, AFFILITATION: String = AFFILIATION, courseAdmission: String): (String, Array[Byte]) =
              (OperationData("mock", TransactionInfo("", "", ""), OperationDataState.PENDING, "", "", "", "", ApprovalList(Seq(), Seq()), ApprovalList(Seq(), Seq())).toJson, courseAdmission.getBytes())

            override def getProposalDropAdmission(certificate: String, AFFILITATION: String = AFFILIATION, admissionId: String): (String, Array[Byte]) =
              (model.operation.OperationData("mock", TransactionInfo("", "", ""), OperationDataState.PENDING, "", "", "", "", ApprovalList(Seq(), Seq()), ApprovalList(Seq(), Seq())).toJson, ("id#" + admissionId).getBytes())

            override def getUnsignedTransaction(proposalBytes: Array[Byte], signature: Array[Byte]): Array[Byte] = {
              "t:".getBytes() ++ proposalBytes
            }

            override def getChaincodeVersion: String = "testVersion"

            override def addAdmission(admission: String): String = ""

            override def dropAdmission(admissionId: String): String = ""

            override lazy val contract: ContractImpl = null
            override lazy val gateway: GatewayImpl = null
            override val username: String = ""
            override val channel: String = ""
            override val chaincode: String = ""
            override val walletPath: Path = null
            override val networkDescriptionPath: Path = null

            override def getProposalGetCourseAdmissions(certificate: String, affiliation: String, enrollmentId: String, courseId: String, moduleId: String): (String, Array[Byte]) = ("", Array.emptyByteArray)

            override def getProposalGetExamAdmissions(certificate: String, affiliation: String, admissionIds: Seq[String], enrollmentId: String, examIds: Seq[String]): (String, Array[Byte]) = ("", Array.emptyByteArray)

            override def getAdmissions(enrollmentId: String, courseId: String, moduleId: String): String = ""
          }
        }
      }
    }

  var client: AdmissionService = _
  var certificate: CertificateServiceStub = _

  private var defaultExam0: Exam = _
  private var defaultExam1: Exam = _
  private var defaultExam2: Exam = _
  private var defaultExam3: Exam = _
  private var defaultExamAdmission0, defaultExamAdmission2, defaultExamAdmission1, defaultExamAdmission4, defaultExamAdmission3: ExamAdmission = _
  private var allExamAdmissions: Seq[ExamAdmission] = _

  private var defaultCourse0 = course0.copy(moduleIds = examReg0.modules.map(_.id))
  private var defaultCourse1 = course1.copy(moduleIds = examReg0.modules.map(_.id))
  private var defaultCourse2 = course2.copy(moduleIds = examReg1.modules.map(_.id))
  private var defaultCourse3 = course3.copy(moduleIds = examReg2.modules.map(_.id))
  private var defaultCourse4 = course3.copy(moduleIds = Seq("NotAnActiveModule"))
  private var defaultCourseAdmission0, defaultCourseAdmission1, defaultCourseAdmission2, defaultCourseAdmission3, defaultCourseAdmission4: CourseAdmission = _
  private var allCourseAdmissions: Seq[CourseAdmission] = _

  override protected def beforeAll(): Unit = {
    client = server.serviceClient.implement[AdmissionService]
    certificate = server.application.certificateService

    //Base Course stuff
    defaultCourse0 = server.application.courseService.addCourse(defaultCourse0)
    defaultCourse1 = server.application.courseService.addCourse(defaultCourse1)
    defaultCourse2 = server.application.courseService.addCourse(defaultCourse2)
    defaultCourse3 = server.application.courseService.addCourse(defaultCourse3)
    defaultCourse4 = server.application.courseService.addCourse(defaultCourse4)

    defaultCourseAdmission0 = CourseAdmission("courseAd0", student0.username + "enrollmentId", "", AdmissionType.Course.toString, defaultCourse0.courseId, defaultCourse0.moduleIds.head)
    defaultCourseAdmission1 = CourseAdmission("courseAd1", student0.username + "enrollmentId", "", AdmissionType.Course.toString, defaultCourse1.courseId, defaultCourse1.moduleIds.apply(1))
    defaultCourseAdmission2 = CourseAdmission("courseAd2", student1.username + "enrollmentId", "", AdmissionType.Course.toString, defaultCourse2.courseId, defaultCourse2.moduleIds.head)
    defaultCourseAdmission3 = CourseAdmission("courseAd3", student1.username + "enrollmentId", "", AdmissionType.Course.toString, defaultCourse3.courseId, defaultCourse3.moduleIds.head)
    defaultCourseAdmission4 = CourseAdmission("courseAd4", student1.username + "enrollmentId", "", AdmissionType.Course.toString, defaultCourse4.courseId, "NotAnActiveModule")

    allCourseAdmissions = Seq(defaultCourseAdmission0, defaultCourseAdmission1, defaultCourseAdmission2, defaultCourseAdmission3)

    //Base Exam stuff
    defaultExam0 = exam0.copy(moduleId = defaultCourse0.moduleIds.head)
    defaultExam1 = exam1.copy(moduleId = defaultCourse1.moduleIds.head)
    defaultExam2 = exam2.copy(moduleId = defaultCourse2.moduleIds.head)
    defaultExam3 = exam3.copy(moduleId = defaultCourse3.moduleIds.head)
    defaultExamAdmission0 = ExamAdmission("examAd0", student0.username + "enrollmentId", "", AdmissionType.Exam.toString, defaultExam0.examId)
    defaultExamAdmission1 = ExamAdmission("examAd1", student1.username + "enrollmentId", "", AdmissionType.Exam.toString, defaultExam0.examId)
    defaultExamAdmission2 = ExamAdmission("examAd2", student0.username + "enrollmentId", "", AdmissionType.Exam.toString, defaultExam1.examId)
    defaultExamAdmission3 = ExamAdmission("examAd3", student2.username + "enrollmentId", "", AdmissionType.Exam.toString, defaultExam2.examId)
    defaultExamAdmission4 = ExamAdmission("examAd4", student2.username + "enrollmentId", "", AdmissionType.Exam.toString, defaultExam3.examId)

    allExamAdmissions = Seq(defaultExamAdmission0, defaultExamAdmission1, defaultExamAdmission2, defaultExamAdmission3, defaultExamAdmission4)

  }

  override protected def afterAll(): Unit = server.stop()

  override protected def afterEach(): Unit = cleanup()

  def prepare(courseAdmissions: CourseAdmission*)(examAdmissions: ExamAdmission*): Unit = {
    server.application.courseAdmissionList ++= courseAdmissions
    server.application.examAdmissionList ++= examAdmissions
  }
  def prepareSeq(courseAdmissions: Seq[CourseAdmission])(examAdmissions: Seq[ExamAdmission]): Unit = {
    server.application.courseAdmissionList ++= courseAdmissions
    server.application.examAdmissionList ++= examAdmissions
  }

  def cleanup(): Unit = {
    server.application.courseAdmissionList = Seq()
    server.application.examAdmissionList = Seq()
  }

  private def asString(unsignedProposal: String) = new String(Base64.getDecoder.decode(unsignedProposal), StandardCharsets.UTF_8)

  "AdmissionService service" should {

    "fetch the hyperledger versions" in {
      client.getHlfVersions.invoke().map { answer =>
        answer shouldBe a[JsonHyperledgerVersion]
      }
    }

    //GET - Course
    "get all course admissions as an admin" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      client.getCourseAdmissions(None, None, None).handleRequestHeader(addAuthorizationHeader()).invoke().map {
        courseAdmissions => courseAdmissions should contain theSameElementsAs allCourseAdmissions
      }
    }

    " get all own course admissions as a student" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      client.getCourseAdmissions(Some(student0.username), None, None).handleRequestHeader(addAuthorizationHeader(student0.username)).invoke().map {
        courseAdmissions => courseAdmissions should contain theSameElementsAs Seq(defaultCourseAdmission0, defaultCourseAdmission1)
      }
    }
    "get a course admissions from an own course as lecturer" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      client.getCourseAdmissions(None, Some(defaultCourse0.courseId), None).handleRequestHeader(addAuthorizationHeader(lecturer0.username)).invoke().map {
        courseAdmissions => courseAdmissions should contain theSameElementsAs Seq(defaultCourseAdmission0)
      }
    }

    "not get all course admissions as a student" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      client.getCourseAdmissions(None, None, None).handleRequestHeader(addAuthorizationHeader(student0.username)).invoke().failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
      }
    }

    "not get a course admission from another student" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      client.getCourseAdmissions(Some(student1.username), None, None).handleRequestHeader(addAuthorizationHeader(student0.username)).invoke().failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
      }
    }

    "not get a course admission from another lecturer" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      client.getCourseAdmissions(None, Some(defaultCourse2.courseId), None).handleRequestHeader(addAuthorizationHeader(lecturer0.username)).invoke().failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
      }
    }

    "not get a course admission if you are not enrolled" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      client.getCourseAdmissions(Some(student0.username + "NotEnrolledYet"), None, None).handleRequestHeader(addAuthorizationHeader(student0.username + "NotEnrolledYet")).invoke().failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.KeyNotFound)
      }
    }

    "not get a course admission with an non-existent course id" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      client.getCourseAdmissions(None, Some("NotFound"), None).handleRequestHeader(addAuthorizationHeader(lecturer0.username)).invoke().failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.KeyNotFound)
      }
    }

    "get all course admissions with specified moduleId as an admin" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      client.getCourseAdmissions(None, None, Some(defaultCourse0.moduleIds.head)).handleRequestHeader(addAuthorizationHeader()).invoke().map {
        courseAdmissions => courseAdmissions should contain theSameElementsAs Seq(defaultCourseAdmission0, defaultCourseAdmission2)
      }
    }

    "get all course admissions with specified username,moduleId and courseID as an student" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      client.getCourseAdmissions(Some(student0.username), Some(defaultCourse0.courseId), Some(defaultCourse0.moduleIds.head)).handleRequestHeader(addAuthorizationHeader(student0.username)).invoke().map {
        courseAdmissions => courseAdmissions should contain theSameElementsAs Seq(defaultCourseAdmission0)
      }
    }

    //GET - Exam
    "get all exam admissions as an admin" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      client.getExamAdmissions(None, None, None).handleRequestHeader(addAuthorizationHeader()).invoke().map {
        examAdmissions => examAdmissions should contain theSameElementsAs allExamAdmissions
      }
    }

    " get all own exam admissions as a student" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      client.getExamAdmissions(Some(student0.username), None, None).handleRequestHeader(addAuthorizationHeader(student0.username)).invoke().map {
        examAdmissions => examAdmissions should contain theSameElementsAs Seq(defaultExamAdmission0, defaultExamAdmission2)
      }
    }

    "not get all exam admissions as a student" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      client.getExamAdmissions(None, None, None).handleRequestHeader(addAuthorizationHeader(student0.username)).invoke().failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
      }
    }

    "not get all exam admissions as a lecturer" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      client.getExamAdmissions(None, None, None).handleRequestHeader(addAuthorizationHeader(lecturer0.username)).invoke().failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
      }
    }

    "not get exam admissions as another student" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      client.getExamAdmissions(Some(student1.username), None, Some("RandomExamId")).handleRequestHeader(addAuthorizationHeader(student0.username)).invoke().failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
      }
    }

    " get all exam admissions from an exam as the lecturer which holds the exam" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      client.getExamAdmissions(None, None, Some(defaultExam0.examId)).handleRequestHeader(addAuthorizationHeader(lecturer0.username)).invoke().map {
        examAdmissions => examAdmissions should contain theSameElementsAs Seq(defaultExamAdmission0, defaultExamAdmission1)
      }
    }

    "not get an exam admission from an exam of another lecturer" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      client.getExamAdmissions(None, None, Some(defaultExam3.examId)).handleRequestHeader(addAuthorizationHeader(lecturer0.username)).invoke().failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.OwnerMismatch)
      }
    }

    "not get an exam admission if you are not enrolled" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      client.getExamAdmissions(Some(student0.username + "NotEnrolledYet"), None, None).handleRequestHeader(addAuthorizationHeader(student0.username + "NotEnrolledYet")).invoke().failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.`type` should ===(ErrorType.KeyNotFound)
      }
    }

    "not get an exam admission with an non-existent exam id" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      client.getExamAdmissions(None, None, Some("NotFound")).handleRequestHeader(addAuthorizationHeader(lecturer0.username)).invoke().map {
        examAdmissions => examAdmissions should contain theSameElementsAs Seq()
      }
    }

    "get the examAdmission with specified admissionId as an admin" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      client.getExamAdmissions(None, Some(defaultExamAdmission0.admissionId + "," + defaultExamAdmission2.admissionId), None).handleRequestHeader(addAuthorizationHeader()).invoke().map {
        examAdmissions => examAdmissions should contain theSameElementsAs Seq(defaultExamAdmission0, defaultExamAdmission2)
      }
    }
    "get the examAdmission with specified username, admissionIds, and examIds as a student" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      client.getExamAdmissions(Some(student0.username), Some(defaultExamAdmission0.admissionId + "," + defaultExamAdmission2.admissionId), Some(defaultExam0.examId)).handleRequestHeader(addAuthorizationHeader(student0.username)).invoke().map {
        examAdmissions => examAdmissions should contain theSameElementsAs Seq(defaultExamAdmission0)
      }
    }

    //POST - negative generalAdd
    "not get a proposal course admission with non empty enrollmentId,admissionId or timestamp" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      // here all three fields are non empty but in return we also get three validation errors
      client.getProposalAddAdmission.handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(defaultCourseAdmission0.copy(enrollmentId = "nonEmpty", admissionId = "nonEmpty", timestamp = "nonEmpty")).failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError].invalidParams.map(_.name) should
            contain theSameElementsAs Seq("admissionId", "enrollmentId", "timestamp")
      }
    }
    "not get a proposal exam admission with non empty enrollmentId,admissionId or timestamp" in {
      prepareSeq(allCourseAdmissions)(allExamAdmissions)
      // here all three fields are non empty but in return we also get three validation errors
      client.getProposalAddAdmission.handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(defaultExamAdmission0.copy(enrollmentId = "nonEmpty", admissionId = "nonEmpty", timestamp = "nonEmpty")).failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError].invalidParams.map(_.name) should
            contain theSameElementsAs Seq("admissionId", "enrollmentId", "timestamp")
      }
    }

    //POST - addCourse
    "get proposal add course admission" in {
      client.getProposalAddAdmission.handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(defaultCourseAdmission0.copy(enrollmentId = "", admissionId = "")).map {
        unsignedProposalJson =>
          asString(unsignedProposalJson.unsignedProposal).fromJson[CourseAdmission]
            .copy(timestamp = "") should ===(defaultCourseAdmission0.copy(admissionId = "", enrollmentId = certificate.get(student0.username).enrollmentId))
      }
    }

    "not get proposal add course admission " in {
      client.getProposalAddAdmission.handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(defaultCourseAdmission0.copy(enrollmentId = "", admissionId = "", moduleId = "moduleNotFound")).failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError].invalidParams should
            contain theSameElementsAs Seq(
              SimpleError("moduleId", "CourseId can not be attributed to module with the given moduleId."),
              SimpleError("courseId", "The module with the given moduleId can not be attributed to the course with the given courseId.")
            )
      }
    }

    "not get proposal add course admission with an inactive moduleId" in {
      client.getProposalAddAdmission.handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(defaultCourseAdmission4.copy(enrollmentId = "", admissionId = "", moduleId = "NotAnActiveModule")).failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError].invalidParams should
            contain theSameElementsAs Seq(
              SimpleError("moduleId", "The given moduleId can not be attributed to an active exam regulation.")
            )
      }
    }

    //POST - addExam
    "get proposal add exam admission" in {
      client.getProposalAddAdmission.handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(defaultExamAdmission0.copy(enrollmentId = "", admissionId = "")).map {
        unsignedProposalJson =>
          asString(unsignedProposalJson.unsignedProposal).fromJson[ExamAdmission]
            .copy(timestamp = "") should ===(defaultExamAdmission0.copy(admissionId = "", enrollmentId = certificate.get(student0.username).enrollmentId))
      }
    }

    //DELETE- dropCourse
    "get proposal drop course admission" in {
      client.getProposalDropAdmission.handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(DropAdmission("id")).map {
        unsignedProposalJson =>
          asString(unsignedProposalJson.unsignedProposal) should ===("id#id")
      }
    }
    "not get proposal drop course admission with an empty admissionId" in {
      client.getProposalDropAdmission.handleRequestHeader(addAuthorizationHeader(student0.username)).invoke(DropAdmission("")).failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError].invalidParams.map(_.name) should
            contain theSameElementsAs Seq("admissionId")
      }
    }
  }
}
