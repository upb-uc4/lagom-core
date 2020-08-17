# [v.0.6.1](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.6.0...user-v0.6.1) (2020-XX-XX)
## Feature
 - Added custom deserialization exception
## Refactor
## Bugfix

# [v.0.6.0](https://github.com/upb-uc4/University-Credits-4.0/compare/user-v0.5.1...user-v0.6.0) (2020-08-17)
## Feature
- Added check in update call if the fields are editable (even for an admin)
- Added phone number field to all users
- Added latestImmatriculation to student
- Added function to update latestImmatriculation
- Removed now obsolet fields (fieldOfStudy, semesterCount, immatriculationStatus)
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
- Fixed ambigous endpoints caused by UC4Service Trait