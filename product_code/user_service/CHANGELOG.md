# [WIP v.0.10.2](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.10.1...user-v0.10.2) (2020-XX-XX)
## Feature
- The Circuit Breaker ignores now UC4NonCriticalExceptions
## Refactor
- Moved configurations to shared
## Bugfix

# [v.0.10.1](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.9.3...user-v0.10.1) (2020-10-15)
## Feature
 - Added thumbnail picture for each profile
 - Added support for gif and webp
 - Added basic image processing
## Refactor
## Bugfix

# [v.0.9.3](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.9.2...user-v0.9.3) (2020-10-09)
## Feature
 - Wrapped Validation in Future to enable timeouts
 - Enabled automatic user registration with Hyperledger
## Refactor
## Bugfix
 - Adjusted maximal profile picture size

# [v.0.9.2](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.9.1...user-v0.9.2) (2020-09-28)
## Feature
 - Added default profile picture
 - Added endpoints to delete profile pictures
## Refactor
## Bugfix
 - PUT of a profile picture returns now a 200
 - Profile pictures gets deleted on user deletion

# [v.0.9.1](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.8.3...user-v0.9.1) (2020-09-28)
## Feature
 - Added Support of Profile Pictures
 - Added Endpoints for Profile Pictures
## Refactor
## Bugfix

# [v.0.8.3](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.8.2...user-v0.8.3) (2020-09-22)
## Feature
 - Added Gzipping in Options Header
## Refactor
 - NotFound in updateUser is now part validation
 - Duplicate in addUser is now part validation
## Bugfix
 - Fixed empty username causing a timeout

# [v.0.8.2](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.8.1...user-v0.8.2) (2020-09-17)
## Feature
## Refactor
## Bugfix
 - Fixed NotFound/Duplicate not being checked before validation
 - Fixed duplicate matriculationIds being allowed
 
# [v.0.8.1](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.8.0...user-v0.8.1) (2020-09-15)
## Feature
- Supports Bearer Authentication with JWT token
 - Enabling Gzipping
## Refactor
## Bugfix
 - Fixed hard coded HTTP Secret

# [v.0.8.0](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.7.1...user-v0.8.0) (2020-09-14)
## Feature
## Refactor
## Bugfix
- Fixed admins being able to create courses with invalid lecturers

# [v.0.7.1](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.7.0...user-v0.7.1) (2020-09-08)
## Feature
 - Merged most endpoints in "students", "lecturers" and "admins" into "users"
## Refactor
## Bugfix

# [v.0.7.0](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.6.0...user-v0.7.0) (2020-09-01)
## Feature
 - Added custom deserialization exception
## Refactor
 - Added ServiceStub for testing to reduce code duplication
 - Added default Users for easier maintainability in testing
 - Added methods for better modularity in testing
## Bugfix
 - Fixed changeLatestImmatriculation being an exposed endpoint

# [v.0.6.0](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.5.1...user-v0.6.0) (2020-08-17)
## Feature
- Added check in update call if the fields are editable (even for an admin)
- Added phone number field to all users
- Added latestImmatriculation to student
- Added function to update latestImmatriculation
- Removed now obsolete fields (fieldOfStudy, semesterCount, immatriculationStatus)
## Refactor
- Splitted editableFields method into two methods, one for permission check, one for possibility check
## Bugfix

# [v.0.5.1](https://github.com/upb-uc4/University-Credits-4.0/compare/v0.5.0...user-v0.5.1) (2020-08-05)
## Feature
- Added predefined standard exceptions to CustomException
- Added functionality that fetching a user returns less personal info, when invoked by a non-Admin that is not the user to fetch
- Added query parameter "usernames" to getAllUsers, getAllStudents, getAllLecturers, getAllAdmins to filter with
## Refactor
- Changed exceptions to use these standard exceptions wherever possible
## Bugfix
- Fixed a bug that allowed houseNumbers to be empty
- Fixed a bug that allowed "01" and "001" as matriculationId's, while not recognising them as the same number
- Fixed ambigous endpoints caused by UC4Service Trait
