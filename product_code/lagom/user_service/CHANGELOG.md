# [v.0.5.1 WIP](https://github.com/upb-uc4/University-Credits-4.0/compare/v0.5.0...user-v0.5.1) (2020-XX-XX)
## Feature
- Add predefined standard exceptions to CustomException
- Add functionality that fetching a user returns less personal info, when invoked by a non-Admin that is not the user to fetch
- Add check in update call if the fields are editable (even for an admin)
## Refactor
- Change exceptions to use these standard exceptions whereever possible
- Split editableFields method into two methods, one for permission check, one for possibility check
## Bugfix
- Fix ambigous endpoints caused by UC4Service Trait