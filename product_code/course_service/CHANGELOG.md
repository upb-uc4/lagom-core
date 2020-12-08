# [v0.14.0](https://github.com/upb-uc4/University-Credits-4.0/compare/course-v0.13.1...course-v0.14.0) (2020-12-08)
## Feature
## Refactor
 - Made course list accessible without authorization
## Bugfix

# [v0.13.1](https://github.com/upb-uc4/University-Credits-4.0/compare/course-v0.12.2...course-v0.13.1) (2020-11-25)
## Feature
## Refactor
 - Rebuilt to be compatible with new user-service API
## Bugfix

# [v0.12.2](https://github.com/upb-uc4/University-Credits-4.0/compare/course-v0.12.1...course-v0.12.2) (2020-11-17)
## Feature
## Refactor
## Bugfix
 - Fixed query parameter in getAllCourses
 - Fixed addCourse fetching all modules instead of only relevant ones
 
# [v0.12.1](https://github.com/upb-uc4/University-Credits-4.0/compare/course-v0.12.0...course-v0.12.1) (2020-11-17)
## Feature
 - Added configurable timeouts in application config
 - Added "moduleIds" to Course object
 - Added query parameter for module Ids
 - Added dependency; CourseService dependant on ExamReg
## Refactor
## Bugfix

# [v0.12.0](https://github.com/upb-uc4/University-Credits-4.0/compare/course-v0.11.1...course-v0.12.0) (2020-11-10)
## Feature
## Refactor
 - Bumped Version of Lagom to 1.6.4
## Bugfix

# [v0.11.1](https://github.com/upb-uc4/University-Credits-4.0/compare/course-v0.11.0...course-v0.11.1) (2020-11-06)
## Feature
 - Added support for ETags
## Refactor
## Bugfix
 - Added all messages to serialization registry

# [v0.11.0](https://github.com/upb-uc4/University-Credits-4.0/compare/course-v0.10.0...course-v0.11.0) (2020-10-26)
## Feature
 - Changed Circuit Breaker to ignore UC4NonCriticalExceptions
 - Added 0 as possible ECTS value
## Refactor
 - Moved configurations to shared
## Bugfix
 - Added minimum size to gzip (512 Byte)

# [v0.10.0](https://github.com/upb-uc4/University-Credits-4.0/compare/course-v0.9.0...course-v0.10.0) (2020-10-12)
## Feature
 - Wrapped Validation in Future to enable timeouts
## Refactor
## Bugfix

# [v0.9.0](https://github.com/upb-uc4/University-Credits-4.0/compare/course-v0.8.2...course-v0.9.0) (2020-10-02)
## Feature
## Refactor
 - Rebuilt to implement new user-service API
## Bugfix

# [v0.8.2](https://github.com/upb-uc4/University-Credits-4.0/compare/course-v0.8.1...course-v0.8.2) (2020-09-22)
## Feature
 - Added Gzipping in Options Header
## Refactor
 - Added NotFound to validationErrors
## Bugfix
 - Fixed empty lecturerId causing an internal 404

# [v0.8.1](https://github.com/upb-uc4/University-Credits-4.0/compare/course-v0.7.1...course-v0.8.1) (2020-09-15)
## Feature
 - Added support for Bearer Authentication with JWT token
 - Enabled Gzipping
## Refactor
## Bugfix
 - Fixed hard coded HTTP Secret

# [v0.7.1](https://github.com/upb-uc4/University-Credits-4.0/compare/course-v0.7.0...course-v0.7.1) (2020-09-01)
## Feature
## Refactor
## Bugfix
 - Fixed adding/updating of courses with non-existing lecturers

# [v0.7.0](https://github.com/upb-uc4/University-Credits-4.0/compare/course-v0.6.0...course-v0.7.0) (2020-09-01)
## Feature
 - Added custom deserialization exceptions 
## Refactor
## Bugfix

# [v0.6.0](https://github.com/upb-uc4/University-Credits-4.0/compare/v0.5.0...course-v0.6.0) (2020-08-17)
## Feature
 - Added predefined standard exceptions to CustomException
## Refactor
 - Changed exceptions to use these standard exceptions whereever possible
## Bugfix
 - Fixed ambigous endpoints caused by UC4Service Trait
 - Fixed search for courseName not being fuzzy
