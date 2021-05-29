### Restaurant - Rest without boilerplate

A small web server for REST apis.

There is clear consensus how a good REST api should look like. Restaurant helps you implement it in a simple way that is
easy to test. Uses Undertow for non-blocking http handling, so it should perform well despite still being new.

```kotlin

fun main() {
    Restaurant(8080) {
        namespace("/api") {
            resources(UsersService())
        }
    }
}

data class User(val id: String?, val name: String/*..all kinds of user fields..*/)
class UsersService : RestService {
    suspend fun index(): List<User> {

    }
    suspend fun create(user: User): User {
        //...
    }

    suspend fun show(userId: Int): User {
        //....
    }

    suspend fun update(userId: Int, user: User): User {
        //..
    }
}
```

This will create these routes

* `GET /api/users` calling `suspend fun index()`
* `GET /api/users/{id}` calling `suspend fun show(userId: Int)`
* `POST /api/users` calling  `suspend fun create(user: User):`
* `PUT /api/users/{id}` calling `suspend fun update(userId: Int, user: User)`

methods can be `suspend` or not, and can declare their incoming and outgoing parameters as data classes that will be
serialized and deserialized with jackson.

### error handling

Exceptions thrown by the service handler are converted to a http error reply via a lambda.

```kotlin
val restaurant = Restaurant(errorHandler = { ex ->
    ErrorReply(
        status = 500,
        body = "sorry"
    )
})
```

### Authentication via JWT

JWT Authentication is super simple, first you need the usual JWT stuff:

```kotlin
object JWTConfig {
    private const val issuer = "https://restaurant.dev/"
    private const val audience = "jwtAudience"
    private val algorithm = Algorithm.HMAC256("psst, secret")!!
    fun makeToken(userId: Long): String = JWT.create()
        .withAudience(audience)
        .withSubject("Authentication")
        .withIssuer(issuer)
        .withClaim("jti", userId)
        .sign(algorithm)!!

    fun makeJwtVerifier(): JWTVerifier = JWT.require(algorithm)
        .withAudience(audience)
        .withIssuer(issuer)
        .build()
}
```

In your Restaurant routing DSL you just wrap the protected resources or handlers:

```kotlin
        val restaurant = Restaurant {
    jwt(JWTConfig.makeJwtVerifier()) {
        get("/handlers/welcome", JWTWelcomeHandler())
    }
}

```

Wrapped handlers can get the jwt info:

```kotlin
class JWTWelcomeHandler : SuspendingHandler {
    override suspend fun handle(exchange: Exchange, context: RequestContext): Response {
        return (response("welcome user " + context[JWTWrapper].getClaim("jti")))
    }
}
```

## coming next:

Readme driven development: all features below this point are not yet implemented.

### nested resources:

```kotlin
namespace("/api") {
    resources(UsersService()) {
        resources(HobbiesService()) // user has many hobbies
    }
}
```

will generate routes like `GET /api/users/10/hobbies/20` or `POST /api/users/10/hobbies`


### Developer friendly

* Show a list of defined routes when a 404 error occurs in development mode.
* Friendly error messages when json can be serialized or deserialized.
* Any stacktrace in undertow or restaurant code that does not show a useful error message should be reported as a bug.
