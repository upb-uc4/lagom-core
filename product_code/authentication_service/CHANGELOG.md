# [v.0.7.1](https://github.com/upb-uc4/University-Credits-4.0/compare/authentication-v0.7.0...authentication-v0.7.1) (2020-09-01)
## Feature
 - JWT Token uses now Lax Same Site restrictuin
## Refactor
## Bugfix

# [v.0.7.0](https://github.com/upb-uc4/University-Credits-4.0/compare/authentication-v0.6.0...authentication-v0.7.0) (2020-09-01)
## Feature
 - Added custom deserialization exceptions
 - Switch to JWT Token
    - Added login endpoint which uses basic to create a refresh and a login token
    - Added refresh endpoint to create a new login token with the refresh token
 - The authentication service is now only needed for the first login 
## Refactor
 - Added ServiceStub for testing to reduce code duplication
 - Added default AuthenticationUsers for easier maintainability in testing
## Bugfix

# [v.0.6.0](https://github.com/upb-uc4/University-Credits-4.0/compare/v0.5.0...authentication-v0.6.0) (2020-08-17)
## Feature
 - Added predefined standard exceptions to CustomException
## Refactor
 - Changed exceptions to use these standard exceptions whereever possible
## Bugfix
 
