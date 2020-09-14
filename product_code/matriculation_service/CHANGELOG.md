# [WIP v.0.8.2](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.8.1...matriculation-v0.8.2) (2020-XX-XX)
## Feature
- Supports Bearer Authentication with JWT token
## Refactor
## Bugfix

# [v.0.8.1](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.7.1...matriculation-v0.8.1) (2020-09-14)
## Feature
 - Added support for sending multiple field of studies
## Refactor
## Bugfix

# [v.0.7.1](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.7.0...matriculation-v0.7.1) (2020-09-08)
## Feature
 - Added full deployment support
## Refactor
## Bugfix
 - Fix wrong forwarding of cookie header

# [v.0.7.0](https://github.com/upb-uc4/University-Credits-4.0/compare/matriculation-v0.6.0...matriculation-v0.7.0) (2020-09-01)
## Feature
 - Added custom deserialization exception
## Refactor
 - Adapted to Hyperleger-API v.0.6.1 The new building blocks of the Hyperledger communication are the `HyperledgerComponent`, `HyperledgerActorFactory` as well as several helper methods bundled in `HyperledgerUtils`.
## Bugfix


# [v.0.6.0](https://github.com/upb-uc4/University-Credits-4.0/compare/v0.5.0...matriculation-v0.6.0) (2020-08-17)
## Feature
 - Added predefined standard exceptions to CustomException
 - Added call to immatriculate a Student for one specific fieldOfStudy + semester
 - Added call to fetch the complete matriculation history of a specific Student
 - Added update call to UserService to cache the latestImmatriculation
## Refactor
 - Changed exceptions to use these standard exceptions wherever possible
## Bugfix
