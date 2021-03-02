package de.upb.cs.uc4.configuration.impl

import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.configuration.api.ConfigurationService
import de.upb.cs.uc4.configuration.model.{ Configuration, JsonHyperledgerNetworkVersion, ValidationConfiguration }
import de.upb.cs.uc4.shared.client.configuration.{ ConfigurationCollection, CourseLanguage, CourseType, ExamType }
import de.upb.cs.uc4.shared.client.exceptions.{ DetailedError, SimpleError, UC4Exception }
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

/** Tests for the ConfigurationService */
class ConfigurationServiceSpec extends AsyncWordSpec with Matchers {

  private val server = ServiceTest.startServer(
    ServiceTest.defaultSetup
  ) { ctx =>
      new ConfigurationApplication(ctx) with LocalServiceLocator {}
    }

  val client: ConfigurationService = server.serviceClient.implement[ConfigurationService]

  "ConfigurationService" should {
    "fetch hl-network version" in {
      client.getHyperledgerNetworkVersion.invoke().map {
        answer => answer should ===(JsonHyperledgerNetworkVersion("unavailable"))
      }
    }

    "get the semester derived from a date" in {
      client.getSemester(Some("2020-10-14")).invoke().map {
        answer =>
          answer.semester should ===("WS2020/21")
      }
    }

    "fail getting the semester with an empty date" in {
      client.getSemester(None).invoke().failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError].invalidParams should contain theSameElementsAs Seq(SimpleError("date", "Query parameter \"date\" must be set."))
      }
    }

    "fail getting the semester with an malformed date" in {
      client.getSemester(Some("1234-12-1")).invoke().failed.map {
        answer =>
          answer.asInstanceOf[UC4Exception].possibleErrorResponse.asInstanceOf[DetailedError].invalidParams should contain theSameElementsAs Seq(SimpleError("date", "Date must be of the following format \"yyyy-mm-dd\"."))
      }
    }

    "get the configuration" in {
      val configuration = Configuration(
        ConfigurationCollection.countries,
        CourseLanguage.All.map(_.toString),
        CourseType.All.map(_.toString),
        ExamType.All.map(_.toString),
        ConfigurationCollection.grades
      )
      client.getConfiguration.invoke().map {
        answer =>
          answer should ===(configuration)
      }
    }

    "get the validation configuration" in {
      client.getValidation.invoke().map {
        answer =>
          answer should ===(ValidationConfiguration.build)
      }
    }
  }
}
