# Lagom Handbook Number 3: The Developer's guide to microservice ascension

## Table of Contents

  * [General](#general)
    * [Services and Service Calls](#services-and-service-calls)
    * [Actors](#actors)
    * [Database](#database)
  * [HL API](#hl-api)
  * [Authentication](#authentication)
  * [Error Handling](#error-handling-uc4-exception-etc)
  * [Kafka Encryption](#kafka-encryption)
  * [External Services](#external-services)
  * [Serialization](#serialization)
    * [Actor Commands and Jackson](#actor-commands-and-jackson)
    * [Play JSON](#play-json)


## General
This document aims to provide all the necessary information to maintain or extend our lagom backend.
In many cases we used the [lagom reference guide](https://www.lagomframework.com/documentation/1.6.x/scala/ReferenceGuide.html) as orientation on how to implement certain parts of the backend.
If possible, we will provide links to the respective parts of the reference guide.

Because the reference guide only shows code snippets, it may be helpful to have the code of our project open as well, to get a feeling on where these snippets belong in the big picture.
There are also [examples](https://www.lagomframework.com/documentation/1.6.x/java/LagomExamples.html) provided by the lagom developers, that may help with certain topics.


### Services and Service Calls
Our design of the services follows the recommended implementation from the 
[lagom reference guide](https://www.lagomframework.com/documentation/1.6.x/scala/ReferenceGuide.html), chapter "Writing Lagom Services".

Similarly, the design of the ServiceCalls is very similar to what one can find in the reference guide. Note that ServiceCall composition is frequently used for the authentication of the service calls. More on that can be found in the [Authentication chapter](#authentication).

### Actors
In our project, we are using two distinct types of actors. The first actor type are the Hyperledger Actors. Information on these can be found in the [Hyperledger API chapter](#hl-api).

The second type are the database actors. These actors are used to store most of the data that is held in the lagom part of our backend.
For that we are using typed, stateful actors, that are implemented in a similar way to the lagom reference guide's implementation.

Extending the database actors can be done by implementing new commands and events.
When doing this, it is important to have the commands and events extend the supplied superclasses, to make them matchable for the command and event handlers of the actors.
To ensure correct serialization of events and commands, see the [section](#serialization) on that topic.

For more information on Akka actors, see the [Akka Documentation](https://doc.akka.io/docs/akka/current/typed/index.html)

### Database
In the backend we use lagoms relational database support to persist actors and actor events, in some cases we also store data directly into the tables (e.g. Authentication data).
In our deployment we are using PostgreSQL as the database, but other SQL databases are also possible. 
For information, on which databases can be used, see the [section](https://www.lagomframework.com/documentation/1.6.x/scala/ChoosingADatabase.html) from the reference guide.

Some queries (e.g. getting all users) require us to fetch all users. 
Since users are stored in actors, it is necessary to have a list of all actor IDs.
This list is implemented via the [lagom read side support](https://www.lagomframework.com/documentation/1.6.x/scala/ReadSideRDBMS.html) for [Slick](https://www.lagomframework.com/documentation/1.6.x/scala/ReadSideSlick.html).
Authentication data is stored in an SQL table via this Slick support.


## HL API
Every service that uses Hyperledger communicates with one contract.
To communicate, a connection to the contract is created using the Hyperledger Scala API.
The communication on our side is realized using Akka actors.
In contrast to our database actors, these actors are stateless and are using [Akka's StatusReply](https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html#generic-response-wrapper) to report errors, instead of responding with rejection messages.

Because all of these actors require some shared functionality, we are providing a basic actor in de.upb.cs.uc4.hyperledger.impl.HyperledgerActor, that can be extended to immediately support some basic commands, like initiating or closing a connection with the blockchain and fetching the chaincode version.
To support queries, add operations, etc. one needs to add a new command that extends de.upb.cs.uc4.hyperledger.impl.commands.HyperledgerCommandSerializable to support Serialization and extend the command handler in the respective behavior with a handler for this command.
For more information on serialization, see the [section](#serialization) on that topic.

Every service has one actor for hyperledger, but the ID is randomly generated, so every pod of that service in the deployment will use its own actor.

Errors from the Hyperledger Scala API will be converted into the same format we are using for lagom.
For that conversion we are supplying a utility (de.upb.cs.uc4.hyperledger.impl.HyperledgerUtils), that provides an error matching, that can be extended to support more error types, and a conversion method.
For more information on the general error handling, see the [Error Handling section](#error-handling-uc4-exception-etc).

Using this communication pattern, communicating with Hyperledger Fabric from a service's implementation is almost identical to the communication with the database. The only difference is that the database actors answer with a rejection message when an error occurs and the hyperledger actors use status replies, so instead of matching the answer to Accepted or Rejected, one has to use map() and recover().


## Authentication
Implementing service calls as in [Implementing Services](https://www.lagomframework.com/documentation/1.6.x/scala/ServiceImplementation.html), we can implement functionality behind GET, POST calls, etc. However, basic calls as written in the guide lack any kind of authentication.  

To provide authentication in our project, use [JSON Web Tokens](https://jwt.io/). The AuthenticationService handles token creation. Using regular password logins as authentication, it provides the jwt to the user. This token contains the username and the role of the user, signed by the service. When another service is called, the token's signature is checked, and the user's identity can be confirmed.
> **_NOTE:_**  An additional list of disgraced tokens is needed, in order to invalidate tokens of deleted users. This functionality is not implemented in our product yet; deleted users still being able to authenticate themselves is a known issue

This checking of the signature is implemented by the service calls "authenticated" and "identifiedAuthenticated" in de.upb.cs.uc4.shared.server.ServiceCallFactory. Both check the signature of the token and confirm, if the role is one of the roles specified in the parameters. If the token is invalid, either one will throw an exception resulting in an HTTP error code 403 - Forbidden.  
The "identifiedAuthenticated" provides the same authentication checks as "authenticated", while also retrieving the username and role from the token and making it available in the body of the service call.

To authenticate a service call, we wrap it in either "authenticated" or "identifiedAuthenticated", which will compose the given service call with the service call implementing the authentication logic. 

The "authenticated" call is used as follows:
```scala
def exampleServiceCall: ServiceCall[Request, Response] = authenticated(AuthenticationRole.Admin) {
    ServerServiceCall { (header, request) =>
      // Regular ServiceCall implementation
      // ...
    }  
}
```
Using "authenticated" with the parameter AuthenticationRole.Admin like above, the service call can only be executed if the user has the role AuthenticationRole.Admin, throwing an authentication error if not.  

The "identifiedAuthenticated" call is used as follows:
```scala
def exampleServiceCall2: ServiceCall[Request, Response] = 
    identifiedAuthenticated(AuthenticationRole.Admin, AuthenticationRole.Lecturer) {
        (username, role) =>
            ServerServiceCall { (header, request) =>
                // Regular ServiceCall implementation
                // optional checks, if username or role fit specific conditions
                // ...
            }  
    }
```
The values "username" and "role" in the above example provide the username and role retrieved fromt the token.
This gives us the freedom to add additional, explicit authentication checks, for which these are required. In our project, a reoccurring check is one that ensures that only admins or the owner of a resource can modify specific resources. This check requires both the username and the role, and typically looks like the following example:
```scala
if (role == AuthenticationRole.Lecturer && exampleResource.owner != username) {
    throw UC4Exception.OwnerMismatch
}
```

## Error Handling (UC4 Exception etc.)
In lagom we use Rest-API conform error codes with different error messages to inform the frontend and thus the user if and what of his query failed. Basically UC4CriticalException and UC4NonCriticalException are the only two exception types we use. Both inherit from UC4Exception. The only difference lies in the situation in which they are used. An UC4CriticalException is an unforeseen exception, mainly an InternalServerError. The other one, the UC4NonCriticalException is used in all other cases. For example if an ValidationError occurs, the validation information gets wrapped in an UC4NonCriticalException. To create an UC4NonCriticalException an ErrorCode and a PossibleErrorResponse is required. The latter is of type UC4Error which in turn has to be one of the 4 ErrorTypes DetailedError, InformativeError, GenericError or Transaction error.

Heres a small overview of the construction of the different Errors
 
```scala
GenericError(`type`: ErrorType, title: String)
InformativeError(`type`: ErrorType, title: String, information: String)
DetailedError(`type`: ErrorType, title: String, invalidParams: Seq[SimpleError])
TransactionError(`type`: ErrorType, title: String, transactionId: String)
```

A DetailedError is the most common error which we use during validation. The ErrorType is an enum which holds all error types we have. For example when a DetailedError is created during an objects validation we use the type ErrorType.Validation. InvalidParams in turn is a Sequence of SimpleError(name: String, reason: String) where each SimpleError describes an parameter on which the validation has failed.

For the other errors the construction remains the quite similar. They have all a type and a title. While GenericError is the most basic error, having no additional parameters an InformativeError has an additional information parameter. The TransactionError is mainly used by Hyperledger and receives a transactionId as third parameter.
 
Example:
Imagine we have a rectangle object which has two parameter namely height and width. We restrict the height to be between 0 and 10cm and the width to be between 2 and 5cm. A user creates an rectangle via Rectangle(12,1) which violates our restrictions. As a result our validation will create a Sequence of SimpleErrors, one SimpleError for each incorrect parameter. The following DetailedError together with the error code 422 will be used in an UC4NonCriticalException which will be thrown afterwards. 

```scala
DetailedError(
    ErrorType.Validation, 
    "Your parameters did not validate", 
    Seq[
        SimpleError("height","height must be between 0 and 10cm"),
        SimpleError("width","width must be between 2 and 5cm")
        ]
)
```

For additional information, all error-related classes are found in the folder
package de.upb.cs.uc4.shared.client.exceptions

One will also see the classes UC4ExceptionSerializable and UC4ExceptionSerializer, where the first one is a tag for the Jackson serializer and the latter is the a concrete implementation of the serialization and deserialization our exceptions. More information about serialization can be found in the [serialization](#serialization) section.




## Kafka Encryption
We use [Kafka](https://kafka.apache.org/) for asymmetric communication over topics. For more information on Kafka in lagom consult the [lagom documentation](https://www.lagomframework.com/documentation/1.6.x/scala/MessageBrokerApi.html).
Using the Kafka as in the lagom documentation, the messages are in plaintext. In order to secure our communication, we want to encrypt and authenticate our messages. To realize that, we defined a wrapper object (package de.upb.cs.uc4.shared.client.kafka.EncryptionContainer), in which we can encapsule encrypted data. To send an object over Kafka, we convert it to json and encrypt the result using AES 128 GCM with HMAC. Then, we wrap the resulting bytes in the EncryptionContainer, and send the object over Kafka the way described in the lagom documentation. Upon receiving the wrapper, the payload is decrypted and deserialized.  
The implementation of the encryption and decryption can be found in the package de.upb.cs.uc4.shared.server.kafka.KafkaEncryptionUtility.


## External Services
- API needs to implemented -> then usable as regular services.

## Serialization
Serialization can be divided into two parts, because two serializers are used.
For actor commands, jackson is used, but for everything else, Play JSON is used.

### Actor Commands and Jackson
Commands of every actor are serialized using Jackson (as recommended by the reference guide).
Jackson requires a marker class to declare, which classes should be serialized.
Therefore, every command type in our project has a class that ends in "Serializable", which must be extended by new commands.
If one wants to add commands to one of our actors, the superclass for every command type already extends the marker class, so one just needs to extend this superclass.
The next step is to declare that marker class in the application config.
In the AuthenticationService, this looks as follows:
```
akka.actor {
  serialization-bindings {
    # commands won't use play-json but Akka's jackson support
    "de.upb.cs.uc4.authentication.impl.commands.AuthenticationCommandSerializable" = jackson-json
  }
}
```

Note that if a command is used to send an object of a polymorphic type (e.g. an object that extends a trait), additional work needs to be done. 
An example can be found in our project, by having a look at de.upb.cs.uc4.user.model.user.User class.
For more information on that topic see the [Akka Documentation on that topic](https://doc.akka.io/docs/akka/current/serialization-jackson.html)

### Play JSON
Classes that need to be serialized using Play JSON are all events, the stateful actors themselves and all objects that are persisted in those events or sent to the frontend.
For information on Play JSON see the [documentation](https://www.playframework.com/documentation/2.8.x/ScalaJson).
Important is that every class that needs to be serialized with Play JSON needs to have a companion object with a format.
For example:
```Scala
case class OnUserCreate(user: User, governmentId: String) extends UserEvent

object OnUserCreate {
  implicit val format: Format[OnUserCreate] = Json.format
}
```
Furthermore, all classes need to be registered to the serializer registry using:
`JsonSerializer[OnUserCreate]`.
For a complete example see one of our serializer registries, like de.upb.cs.uc4.user.impl.UserSerializerRegistry.

Similarly to Jackson, polymorphic types are a bit more complicated.
Again, the de.upb.cs.uc4.user.model.user.User class is an example for that.
For this class, the format is more complicated, because one needs to implement a function that decides which subclasses serializer should be used.
For the User, we decide that by considering the "role" field of the received object for the deserialization. 
The serialization can be done by a simple matching, because the object's type is known.

### External services
External services which are not part of the system can be integrated to allow straightforward communication between them and the lagom system. Examples for such unmanaged services are the: `ìmage_processing` and the `pdf_processing` service. These services, when deployed, are capsuled in their own docker container and provide a Rest-API. lagom will then communicate with these services through HTTP. To integrate an unmanaged service, we need to define its API almost identical to a regular lagom service. The REST-paths specified in the service descriptor correspond to the Rest-paths the external service offers. Do not forget to include the service API in the `build.sbt`. The external service in then registered in the service locator by extending `lagomUnmanagedServices` map in the `build.sbt`:

```scala
lagomUnmanagedServices in ThisBuild := Map(
  "imageprocessing" -> sys.env.getOrElse("IMAGE_PROCESSING", "http://localhost:9020"),
  "pdfprocessing" -> sys.env.getOrElse("PDF_PROCESSING", "http://localhost:9030")
)
```
This ensures that the Lagom framework correctly routes requests to the service. Internal service cannot distinguish that an service is external and therefore communicate in such a way as it would be a internal lagom service.