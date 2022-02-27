[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.christophsturm.restaurant/restaurant/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.christophsturm.restaurant/restaurant)
[![Github CI](https://github.com/christophsturm/restaurant/workflows/CI/badge.svg)](https://github.com/christophsturm/restaurant/actions)

### Restaurant - Rest Without Boilerplate

A small web server with focus on testability. Supports coroutines, has a nice low-level-api, and a high-level-api for
rest services. Uses Undertow for non-blocking http handling, so it should perform well despite still being new.

Available from Maven Central.

```kotlin
dependencies {
    implementation("com.christophsturm.restaurant:restaurant-core:0.0.5")
}
```

### High Level API

There is clear consensus how a good REST api should look like. Restaurant helps you implement it in a simple way that is
easy to test.

```kotlin

fun main() {
    Restaurant(8080) {
        namespace("/api") {
            resources(UsersService())
        }
    }
}

data class User(val id: String?, val name: String/*..all kinds of user fields..*/)

@Suppress("RedundantSuspendModifier")
class UsersService : RestService {
    suspend fun index(): List<User> {

    }
    suspend fun create(user: User): User {
    }

    suspend fun show(userId: Int): User {
    }

    suspend fun update(userId: Int, user: User): User {
    }
}
```

This will create these routes

* `GET /api/users` calling `suspend fun index()`
* `GET /api/users/{id}` calling `suspend fun show(userId: Int)`
* `POST /api/users` calling  `suspend fun create(user: User):`
* `PUT /api/users/{id}` calling `suspend fun update(userId: Int, user: User)`

methods can be `suspend` or not, and can declare their incoming and outgoing parameters as data classes that will be
serialized and deserialized with jackson. Your service class has no dependencies on any http server, and should be super
easy to test.

### The Low Level API

There is also a really nice low level api if you need more flexibility.

```kotlin
class ReverserService : SuspendingHandler {
    override suspend fun handle(exchange: Exchange, requestContext: RequestContext): Response {
        return (response(ByteBuffer.wrap(exchange.readBody().reversedArray())))
    }
}
//...
Restaurant {
    route(Method.POST, "/handlers/reverser", ReverserService())
}

val response = request(restaurant, "/handlers/reverser") { post("""jakob""".toRequestBody()) }
expectThat(response) {
    get { statusCode() }.isEqualTo(200)
    get { body }.isNotNull().get { string() }.isEqualTo("bokaj")
}

```

### Exception Handling

Exceptions thrown by the service handler are converted to a http error reply via a lambda.

```kotlin
val restaurant = Restaurant(errorHandler = { ex -> response(500, "sorry") })
```

### Default Routing

You can install a default handler that is invoked when no other route is found.
```kotlin
it("calls default handler if no suitable route is found") {
    val restaurant =
        Restaurant(defaultHandler = { _, _ -> response(418, "not found but anyway I'm teapot") }) { }
    expectThat(request(restaurant, "/not-found")) {
        get { statusCode() }.isEqualTo(418)
        get { body }.isNotNull().get { string() }.isEqualTo("not found but anyway I'm teapot")
    }
}

```

### Authentication Via JWT

JWT Authentication is super simple, first you need the usual JWT stuff.

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

## Coming Next:

Readme driven development: The features below this point are not ready yet.

### Nested Resources:

```kotlin
namespace("/api") {
    resources(UsersService()) {
        resources(HobbiesService()) // user has many hobbies
    }
}
```

will generate routes like `GET /api/users/10/hobbies/20` or `POST /api/users/10/hobbies`

### Developer Friendly

* Show a list of defined routes when a 404 error occurs in development mode.
* Friendly error messages when json can not be serialized or deserialized.
* Any stacktrace in undertow or restaurant code that does not show a useful error message is a bug.
