package de.upb.cs.uc4.configuration.impl

import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import de.upb.cs.uc4.configuration.api.ConfigurationService
import de.upb.cs.uc4.configuration.model.{ Configuration, JsonHyperledgerNetworkVersion, ValidationConfiguration }
import de.upb.cs.uc4.shared.client.configuration.{ ConfigurationCollection, CourseLanguage, CourseType }
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

    "get the configuration" in {
      val configuration = Configuration(
        ConfigurationCollection.countries,
        CourseLanguage.All.map(_.toString),
        CourseType.All.map(_.toString)
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
