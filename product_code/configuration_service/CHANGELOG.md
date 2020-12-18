# [v0.14.2](https://github.com/upb-uc4/University-Credits-4.0/compare/configuration-v0.14.1...configuration-v0.14.2) (2020-12-17)
## Feature
## Refactor
- Renamed endpoint "version/hyperledger" to "version/hyperledger-network",
    as the endpoint does not return the same json object that any other "version/hyperledger" endpoint returns
## Bugfix

# [v0.14.1](https://github.com/upb-uc4/University-Credits-4.0/compare/configuration-v0.13.1...configuration-v0.14.1) (2020-12-14)
## Feature
## Refactor
- Renamed "versionNumber" into "serviceVersion"
## Bugfix

# [v0.13.1](https://github.com/upb-uc4/University-Credits-4.0/compare/configuration-v0.13.0...configuration-v0.13.1) (2020-12-04)
## Feature
## Refactor
 - Remove now obsolete fields of study from configuration object
## Bugfix

# [v0.13.0](https://github.com/upb-uc4/University-Credits-4.0/compare/configuration-v0.12.1...configuration-v0.13.0) (2020-11-23)
## Feature
 - Added configurable timeouts in application config
## Refactor
## Bugfix

# [v0.12.1](https://github.com/upb-uc4/University-Credits-4.0/compare/configuration-v0.12.0...configuration-v0.12.1) (2020-11-11)
## Feature
 - Added error messages for every regex in ValidationConfiguration
## Refactor
## Bugfix

# [v0.12.0](https://github.com/upb-uc4/University-Credits-4.0/compare/configuration-v0.11.1...configuration-v0.12.0) (2020-11-10)
## Feature
## Refactor
 - Bumped Version of Lagom to 1.6.4
## Bugfix

# [v0.11.1](https://github.com/upb-uc4/University-Credits-4.0/compare/configuration-v0.11.0...configuration-v0.11.1) (2020-11-06)
## Feature
 - Added support for ETags
## Refactor
## Bugfix
 - Added all messages to serialization registry

# [v0.11.0](https://github.com/upb-uc4/University-Credits-4.0/compare/configuration-v0.10.1...configuration-v0.11.0) (2020-10-26)
## Feature
## Refactor
- Changed the validation regex object to include more regexes
## Bugfix
- Added minimum size to gzip (512 Byte)

# [v0.10.1](https://github.com/upb-uc4/University-Credits-4.0/compare/configuration-v0.10.1...configuration-v0.10.1) (2020-20-10)
## Feature
- Created ConfigurationService with endpoints for:
    - fetching semester based on date
    - fetching regular expressions used for validation
    - fetching configuration data
## Refactor
- Moved configurations to shared
## Bugfix
 
