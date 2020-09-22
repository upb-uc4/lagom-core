# [v.0.8.2](https://github.com/upb-uc4/University-Credits-4.0/compare/course-v0.8.1...course-v0.8.2) (2020-09-22)
## Feature
 - Added Gzipping in Options Header
## Refactor
 - Added NotFound to validationErrors
## Bugfix
 - Fixed empty lecturerId causing an internal 404

# [v.0.8.1](https://github.com/upb-uc4/University-Credits-4.0/compare/course-v0.7.1...course-v0.8.1) (2020-09-15)
## Feature
- Supports Bearer Authentication with JWT token
 - Enabling Gzipping
## Refactor
## Bugfix
 - Fix hard coded HTTP Secret

# [v.0.7.1](https://github.com/upb-uc4/University-Credits-4.0/compare/course-v0.7.0...course-v0.7.1) (2020-09-01)
## Feature
## Refactor
## Bugfix
 - Fixed adding/updating of courses with non-existing lecturers

# [v.0.7.0](https://github.com/upb-uc4/University-Credits-4.0/compare/course-v0.6.0...course-v0.7.0) (2020-09-01)
## Feature
 - Added custom deserialization exceptions 
## Refactor
## Bugfix

# [v.0.6.0](https://github.com/upb-uc4/University-Credits-4.0/compare/v0.5.0...course-v0.6.0) (2020-08-17)
## Feature
 - Added predefined standard exceptions to CustomException
## Refactor
 - Changed exceptions to use these standard exceptions whereever possible
## Bugfix
- Fixed ambigous endpoints caused by UC4Service Trait
- Fixed search for courseName not being fuzzy
