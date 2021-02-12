# [v0.17.1](https://github.com/upb-uc4/University-Credits-4.0/compare/authentication-v0.14.1...authentication-v0.17.1) (2021-XX-XX)
## Feature
## Refactor
## Bugfix
 - Removed lecturer and student from seeding

# [v0.14.1](https://github.com/upb-uc4/University-Credits-4.0/compare/authentication-v0.13.1...authentication-v0.14.1) (2020-12-14)
## Feature
## Refactor
- Renamed "versionNumber" into "serviceVersion"
## Bugfix

# [v0.13.1](https://github.com/upb-uc4/University-Credits-4.0/compare/authentication-v0.13.0...authentication-v0.13.1) (2020-11-26)
## Feature
## Refactor
 - Rebuilt to be compatible with new user-service API
## Bugfix

# [v0.13.0](https://github.com/upb-uc4/University-Credits-4.0/compare/authentication-v0.12.0...authentication-v0.13.0) (2020-11-23)
## Feature
 - Added configurable timeouts in application config
## Refactor
## Bugfix

# [v0.12.0](https://github.com/upb-uc4/University-Credits-4.0/compare/authentication-v0.11.0...authentication-v0.12.0) (2020-11-10)
## Feature
 - Added support for ETags
## Refactor
 - Bumped Version of Lagom to 1.6.4
## Bugfix
 - Added all messages to serialization registry

# [v0.11.0](https://github.com/upb-uc4/University-Credits-4.0/compare/authentication-v0.10.0...authentication-v0.11.0) (2020-10-26)
## Feature
 - Changed Circuit Breaker to ignore UC4NonCriticalExceptions
 - Added encrypted Kafka topics
## Refactor
 - Moved configurations to shared
## Bugfix
 - Added minimum size to gzip (512 Byte)

# [v0.10.0](https://github.com/upb-uc4/University-Credits-4.0/compare/authentication-v0.9.0...authentication-v0.10.0) (2020-10-12)
## Feature
 - Wrapped Validation in Future to enable timeouts
## Refactor
## Bugfix

# [v0.9.0](https://github.com/upb-uc4/University-Credits-4.0/compare/authentication-v0.8.2...authentication-v0.9.0) (2020-09-28)
## Feature
## Refactor
 - Refactored tests to Unit-Tests
## Bugfix
 - Fixed bug that prevented authentication users being deleted from the tables
 - Fixed bug that lead to a wrong error being shown when the refresh token was missing for machine users


# [v0.8.2](https://github.com/upb-uc4/University-Credits-4.0/compare/authentication-v0.8.1...authentication-v0.8.2) (2020-09-22)
## Feature
 - Added Gzipping in Options Header
## Refactor
 - Created more precise error code for missing refresh token
## Bugfix

# [v0.8.1](https://github.com/upb-uc4/University-Credits-4.0/compare/authentication-v0.7.1...authentication-v0.8.1) (2020-09-15)
## Feature
 - Enabled Machine User Login with Bearer Authentication
 - Enabled Gzipping
## Refactor
## Bugfix
 - Fixed hard coded HTTP Secret

# [v0.7.1](https://github.com/upb-uc4/University-Credits-4.0/compare/authentication-v0.7.0...authentication-v0.7.1) (2020-09-01)
## Feature
 - Changed JWT Tokens to Strict Same Site restriction again
## Refactor
## Bugfix

# [v0.7.0](https://github.com/upb-uc4/University-Credits-4.0/compare/authentication-v0.6.0...authentication-v0.7.0) (2020-09-01)
## Feature
 - Added custom deserialization exceptions
 - Switched to JWT Token
    - Added login endpoint which uses basic to create a refresh and a login token
    - Added refresh endpoint to create a new login token with the refresh token
## Refactor
 - Refactored authentication service to only be necessary for the first login 
 - Added ServiceStub for testing to reduce code duplication
 - Added default AuthenticationUsers for easier maintainability in testing
## Bugfix

# [v0.6.0](https://github.com/upb-uc4/University-Credits-4.0/compare/v0.5.0...authentication-v0.6.0) (2020-08-17)
## Feature
 - Added predefined standard exceptions to CustomException
## Refactor
 - Changed exceptions to use these standard exceptions whereever possible
## Bugfix
 
