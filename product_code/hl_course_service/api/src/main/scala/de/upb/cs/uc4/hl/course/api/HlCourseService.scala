package de.upb.cs.uc4.hl.course.api

import de.upb.cs.uc4.course.api.CourseService

/** The HlCourseService interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the HlCourseService.
  */
trait HlCourseService extends CourseService {
  /** Prefix for the path for the endpoints, a name/identifier for the service*/
  override val pathPrefix = "/hl-course-management"
  override val name = "hlcourse"
}