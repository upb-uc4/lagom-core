# [v.0.5.1 WIP](https://github.com/upb-uc4/University-Credits-4.0/compare/v0.5.0...course-v0.5.1) (2020-XX-XX)
## Feature
 - Add predefined standard exceptions to CustomException
## Refactor
 - Change exceptions to use these standard exceptions whereever possible
## Bugfix
- Fixed ambigous endpoints caused by UC4Service Trait
- Fixed search for courseName not being fuzzy
- Fixed a bug that allowed houseNumbers to be empty
- Fixed a bug that allowed "01" and "001" as matriculationId's, while not recognising them as the same number