# [v0.15.3](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.15.2...user-v0.15.3) (2020-XX-XX)
## Feature
- Add role to user creation topic for addition to hyperledger groups
## Refactor
## Bugfix

# [v0.15.2](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.15.1...user-v0.15.2) (2021-01-06)
## Feature
## Refactor
## Bugfix
- Fixed bug that prevented lecturers from deleting themselves

# [v0.15.1](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.14.1...user-v0.15.1) (2021-01-06)
## Feature
## Refactor
- Added more precise logs for errors in topics
- Enabled users to request the deletion of their accounts
## Bugfix
- Added missing catch in user delete topic

# [v0.14.1](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.13.3...user-v0.14.1) (2020-12-14)
## Feature
## Refactor
- Moved default data to the deployment
- Renamed "versionNumber" into "serviceVersion"
## Bugfix

# [v0.13.3](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.13.2...user-v0.13.3) (2020-11-26)
## Feature
- Replaced DELETE call with soft delete with different bahaviour; Upon soft deletion; the AuthUser is fully deleted and the CertUser is fully deleted (if a non-lecturer is soft-deleted). If a lecturer is deleted CertUser retains enrollmentId and certificate
- Added endpoint for the "old" deletion, with a DELETE on /users/{username}/force
- Added "isActive" flag to User object, which is "true" on creation, and "false" after soft-deletion
- Added Query parameter "only_active" to all "getAll" calls
- Split deletion topic into two, one for auth and one for cert service, since dependencies do not allow to send the role in a topic to auth service
## Refactor
## Bugfix

# [v0.13.2](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.13.1...user-v0.13.2) (2020-11-26)
## Feature
## Refactor
 - Changed PostMessageUser to use proper inheritance
## Bugfix

# [v0.13.1](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.13.0...user-v0.13.1) (2020-11-25)
## Feature
 - Added governmentId to PostMessageUser
 - Added enrollmentIdSecret to user object
 - Changed default users accordingly, default Users govId is "governmentIdStudent", enrollmentIdSecret is base64 encoding of "studentstudent", "lecturerlecturer" or "adminadmin"
## Refactor
## Bugfix

# [v0.13.0](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.12.0...user-v0.13.0) (2020-11-23)
## Feature
 - Added configurable timeouts in application config
## Refactor
## Bugfix

# [v0.12.0](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.11.1...user-v0.12.0) (2020-11-10)
## Feature
## Refactor
 - Bumped Version of Lagom to 1.6.4
## Bugfix

# [v0.11.1](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.11.0...user-v0.11.1) (2020-11-06)
## Feature
 - Added support for ETags
## Refactor
 - Changed email addresses, first names and last names for the default users
 - Changed toPublic to fill in the public information, instead of removing the private information
## Bugfix
 - Added all messages to serialization registry
 
# [v0.11.0](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.10.1...user-v0.11.0) (2020-10-26)
## Feature
 - Changed the Circuit Breaker to ignore UC4NonCriticalExceptions
 - Added support for profile pictures to be properly cropped and stripped of their metadata
 - Added conversion for all image types to jpegs for bandwidth reasons
 - Added encrypted Kafka topics
## Refactor
 - Moved configurations to shared
## Bugfix
 - Added minimum size to gzip (512 Byte)

# [v0.10.1](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.9.3...user-v0.10.1) (2020-10-15)
## Feature
 - Added thumbnail picture for each profile
 - Added support for gif and webp
 - Added basic image processing
## Refactor
## Bugfix

# [v0.9.3](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.9.2...user-v0.9.3) (2020-10-09)
## Feature
 - Wrapped Validation in Future to enable timeouts
 - Enabled automatic user registration with Hyperledger
## Refactor
## Bugfix
 - Adjusted maximal profile picture size

# [v0.9.2](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.9.1...user-v0.9.2) (2020-09-28)
## Feature
 - Added default profile picture
 - Added endpoints to delete profile pictures
## Refactor
## Bugfix
 - Fixed PUT of a profile picture to return 200
 - Fixed profile pictures to get deleted on user deletion

# [v0.9.1](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.8.3...user-v0.9.1) (2020-09-28)
## Feature
 - Added Support of Profile Pictures
 - Added Endpoints for Profile Pictures
## Refactor
## Bugfix

# [v0.8.3](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.8.2...user-v0.8.3) (2020-09-22)
## Feature
 - Added Gzipping in Options Header
## Refactor
 - Changed NotFound in updateUser to be part of the validation
 - Changed Duplicate in addUser to be part of the validation
## Bugfix
 - Fixed empty username causing a timeout

# [v0.8.2](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.8.1...user-v0.8.2) (2020-09-17)
## Feature
## Refactor
## Bugfix
 - Fixed NotFound/Duplicate not being checked before validation
 - Fixed duplicate matriculationIds being allowed
 
# [v0.8.1](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.8.0...user-v0.8.1) (2020-09-15)
## Feature
 - Added support for Bearer Authentication with JWT token
 - Enabled Gzipping
## Refactor
## Bugfix
 - Fixed hard coded HTTP Secret

# [v0.8.0](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.7.1...user-v0.8.0) (2020-09-14)
## Feature
## Refactor
## Bugfix
 - Fixed admins being able to create courses with invalid lecturers

# [v0.7.1](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.7.0...user-v0.7.1) (2020-09-08)
## Feature
 - Merged most endpoints in "students", "lecturers" and "admins" into "users"
## Refactor
## Bugfix

# [v0.7.0](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.6.0...user-v0.7.0) (2020-09-01)
## Feature
 - Added custom deserialization exception
## Refactor
 - Added ServiceStub for testing to reduce code duplication
 - Added default Users for easier maintainability in testing
 - Added methods for better modularity in testing
## Bugfix
 - Fixed changeLatestImmatriculation being an exposed endpoint

# [v0.6.0](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.5.1...user-v0.6.0) (2020-08-17)
## Feature
 - Added check in update call if the fields are editable (even for an admin)
 - Added phone number field to all users
 - Added latestImmatriculation to student
 - Added function to update latestImmatriculation
 - Removed obsolete fields (fieldOfStudy, semesterCount, immatriculationStatus)
## Refactor
 - Split editableFields method into two methods, one for permission check, one for possibility check
## Bugfix

# [v0.5.1](https://github.com/upb-uc4/University-Credits-4.0/compare/v0.5.0...user-v0.5.1) (2020-08-05)
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
