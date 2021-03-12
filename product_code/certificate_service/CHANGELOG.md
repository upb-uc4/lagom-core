# [WIP - v0.19.1](https://github.com/upb-uc4/University-Credits-4.0/compare/certificate-v0.18.1...certificate-v0.19.1) (2021-XX-XX)
## Feature
 - Update to HLF-API v0.18.0
## Refactor
## Bugfix

# [v0.18.1](https://github.com/upb-uc4/University-Credits-4.0/compare/certificate-v0.16.2...certificate-v0.18.1) (2021-02-18)
## Feature
## Refactor
- Allowed students to fetch their own username, given the enrollmentId
## Bugfix

# [v0.16.2](https://github.com/upb-uc4/University-Credits-4.0/compare/certificate-v0.16.1...certificate-v0.16.2) (2021-01-28)
## Feature
- Changed enrollmentId fetching and username fetching to take multiple parameters, and renamed endpoint
## Refactor
## Bugfix

# [v0.16.1](https://github.com/upb-uc4/University-Credits-4.0/compare/certificate-v0.14.1...certificate-v0.16.1) (2021-01-28)
## Feature
 - Added endpoint for fetching username given an enrollmentId
## Refactor
## Bugfix

# [v0.14.1](https://github.com/upb-uc4/University-Credits-4.0/compare/certificate-v0.13.1...certificate-v0.14.1) (2020-12-14)
## Feature
- Added Version endpoint for Hyperledger
## Refactor
- Renamed "versionNumber" into "serviceVersion"
## Bugfix

# [v0.13.1](https://github.com/upb-uc4/University-Credits-4.0/compare/certificate-v0.13.0...certificate-v0.13.1) (2020-11-26)
## Feature
 - Upon soft deletion of a user the CertUser is fully deleted if a non-lecturer is soft-deleted. If a lecturer is deleted, the CertUser retains enrollmentId and certificate.
## Refactor
## Bugfix

# [v0.13.0](https://github.com/upb-uc4/University-Credits-4.0/compare/certificate-v0.12.0...certificate-v0.13.0) (2020-11-23)
## Feature
 - Added configurable timeouts in application config
## Refactor
## Bugfix

# [v0.12.0](https://github.com/upb-uc4/University-Credits-4.0/compare/certificate-v0.11.2...certificate-v0.12.0) (2020-11-10)
## Feature
## Refactor
 - Bumped Version of Lagom to 1.6.4
## Bugfix

# [v0.11.2](https://github.com/upb-uc4/University-Credits-4.0/compare/certificate-v0.11.1...certificate-v0.11.2) (2020-11-06)
## Feature
## Refactor
## Bugfix
 - Added all messages to serialization registry
 
# [v0.11.1](https://github.com/upb-uc4/University-Credits-4.0/compare/certificate-v0.11.0...certificate-v0.11.1) (2020-11-05)
## Feature
 - Added support for ETags
## Refactor
## Bugfix
 - Added certificate state deletion if corresponding user gets deleted

# [v0.11.0](https://github.com/upb-uc4/University-Credits-4.0/compare/certificate-v0.10.3...certificate-v0.11.0) (2020-10-26)
## Feature
 - Changed Circuit Breaker to ignore UC4NonCriticalExceptions
 - Added support for new production deployment
 - Added encrypted Kafka topics
## Refactor
## Bugfix
 - Added minimum size to gzip (512 Byte)

# [v0.10.3](https://github.com/upb-uc4/University-Credits-4.0/compare/certificate-v0.10.2...certificate-v0.10.3) (2020-10-15)
## Feature
## Refactor
## Bugfix
 - Fixed wrong response code of 202 being sent, instead of 201

# [v0.10.2](https://github.com/upb-uc4/University-Credits-4.0/compare/certificate-v0.10.1...certificate-v0.10.2) (2020-10-13)
## Feature
## Refactor
## Bugfix
 - Fixed missing OPTIONS call
 - Added exception handling for hyperledger exceptions
 - Added better and correct validation for csr

# [v0.10.1](https://github.com/upb-uc4/University-Credits-4.0/compare/certificate-v0.9.2...certificate-v0.10.1) (2020-10-13)
## Feature
## Refactor
## Bugfix
 - Fixed missing enrollment

# [v0.9.2](https://github.com/upb-uc4/University-Credits-4.0/compare/certificate-v0.9.1...certificate-v0.9.2) (2020-10-09)
## Feature
## Refactor
## Bugfix
 - Fixed wrong ENV variable


# [v0.9.1](https://github.com/upb-uc4/University-Credits-4.0/compare/certificate-v0.9.1...certificate-v0.9.1) (2020-10-09)
## Feature
 - Added functionality for 
    - user registration and enrollment (with Hyperledger)
    - storing of encrypted private keys
    - fetching certificates
    - fetching the encrypted private keys
    - fetching the enrollmentId
## Refactor
## Bugfix
