# [WIP - v0.14.1](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.13.2...matriculation-v0.14.1) (2020-XX-XX)
## Feature
 - Added Version endpoint for Hyperledger
## Refactor
- Renamed "versionNumber" into "serviceVersion"
## Bugfix

# [v0.13.2](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.13.1...matriculation-v0.13.2) (2020-12-04)
## Feature
 - Added check in "getProposal" call on endpoint "/matriculation/:username/proposal",
 that confirms that the matriculation message contains fieldOfStudies that correspond to
 active examination regulations, as provided by the ExamregService
## Refactor
## Bugfix

# [v0.13.1](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.12.1...matriculation-v0.13.1) (2020-11-25)
## Feature
## Refactor
 - Rebuilt to be compatible with new user-service API
## Bugfix

# [v0.12.1](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.12.0...matriculation-v0.12.1) (2020-11-17)
## Feature
 - Added configurable timeouts in application config
 - Added old endpoints for backwards compatibility
## Refactor
## Bugfix

# [v0.12.0](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.11.3...matriculation-v0.12.0) (2020-11-10)
## Feature
## Refactor
 - Bumped Version of Lagom to 1.6.4
## Bugfix

# [v0.11.3](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.11.2...matriculation-v0.11.3) (2020-11-06)
## Feature
## Refactor
## Bugfix
 - Added all messages to serialization registry

# [v0.11.2](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.11.1...matriculation-v0.11.2) (2020-11-05)
## Feature
 - Implemented new hyperledger API
## Refactor
## Bugfix

# [v0.11.1](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.11.0...matriculation-v0.11.1) (2020-11-05)
## Feature
 - Changed Matriculation to the new proposal system
 - Added support for ETags
## Refactor
## Bugfix

# [v0.11.0](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.10.1...matriculation-v0.11.0) (2020-10-26)
## Feature
 - Changed Circuit Breaker to ignore UC4NonCriticalExceptions
 - Added support for new production deployment
## Refactor
 - Moved configurations to shared
## Bugfix
 - Added minimum size to gzip (512 Byte)

# [v0.10.1](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.10.0...matriculation-v0.10.1) (2020-10-15)
## Feature
 - Added pseudonymisation by using enrollmentId instead of matriculationId
 - Removed FirstName, LastName and BirthDate from MatriculationData
## Refactor
## Bugfix

# [v0.10.0](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.9.0...matriculation-v0.10.0) (2020-10-12)
## Feature
 - Wrapped Validation in Future to enable timeouts
## Refactor
## Bugfix

# [v0.9.0](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.8.4...matriculation-v0.9.0) (2020-10-02)
## Feature
## Refactor
 - Rebuilt to implement new user-service API
## Bugfix

# [v0.8.4](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.8.3...matriculation-v0.8.4) (2020-09-22)
## Feature
 - Added Gzipping in Options Header
## Refactor
## Bugfix

# [v0.8.3](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.8.2...matriculation-v0.8.3) (2020-09-17)
## Feature
## Refactor
## Bugfix
 - Fixed InternalServerError being thrown where a NotFound was expected

# [v0.8.2](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.8.1...matriculation-v0.8.2) (2020-09-15)
## Feature
 - Added support for Bearer Authentication with JWT token
 - Enabled Gzipping
## Refactor
## Bugfix
 - Fixed hard coded HTTP Secret

# [v0.8.1](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.7.1...matriculation-v0.8.1) (2020-09-14)
## Feature
 - Added support for sending multiple field of studies
## Refactor
## Bugfix

# [v0.7.1](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.7.0...matriculation-v0.7.1) (2020-09-08)
## Feature
 - Added full deployment support
## Refactor
## Bugfix
 - Fixed wrong forwarding of cookie header

# [v0.7.0](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.6.0...matriculation-v0.7.0) (2020-09-01)
## Feature
 - Added custom deserialization exception
## Refactor
 - Adapted to Hyperleger-API v.0.6.1 The new building blocks of the Hyperledger communication are the `HyperledgerComponent`, `HyperledgerActorFactory` as well as several helper methods bundled in `HyperledgerUtils`.
## Bugfix


# [v0.6.0](https://github.com/upb-uc4/University-Credits-4.0/compare/v0.5.0...matriculation-v0.6.0) (2020-08-17)
## Feature
 - Added predefined standard exceptions to CustomException
 - Added call to immatriculate a Student for one specific fieldOfStudy + semester
 - Added call to fetch the complete matriculation history of a specific Student
 - Added update call to UserService to cache the latestImmatriculation
## Refactor
 - Changed exceptions to use these standard exceptions wherever possible
## Bugfix
