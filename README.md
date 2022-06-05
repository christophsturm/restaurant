[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.christophsturm.restaurant/restaurant-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.christophsturm.restaurant/restaurant)
[![Github CI](https://github.com/christophsturm/restaurant/workflows/CI/badge.svg)](https://github.com/christophsturm/restaurant/actions)
[![codecov](https://codecov.io/gh/christophsturm/restaurant/branch/main/graph/badge.svg?token=3EV51LYGSC)](https://codecov.io/gh/christophsturm/restaurant)

### Restaurant - Rest Without Boilerplate

A small web server with focus on testability. Supports coroutines, has a nice low-level-api, and a high-level-api for
rest services. Uses Undertow for non-blocking http handling, so it should perform well despite still being new. Also
includes a http client based on the java 11 http client.

Available from Maven Central.

```kotlin
dependencies {
    implementation("com.christophsturm.restaurant:restaurant-core:0.0.6")
}
```

### High Level API

Restaurant helps you implement REST apis in a simple way that is easy to test, and have your handlers not depend on any webserver classes.

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

#### Streaming

You can stream responses from your service class by using a
flow: [StreamingExample](rest/src/test/kotlin/restaurant/rest/StreamingExample.kt)

```kotlin
class StreamingService : RestService {
    fun index(): Flow<User> {
        return flow {
            emit(User("5", "userName"))
            delay(10)
            emit(User("6", "otherUserName"))
        }
    }
}

val restaurant = Restaurant {
    resources(StreamingService())
}

val response: RestaurantResponse<Flow<String>> = restaurant.streamRequest("/streaming")
expectThat(response) {
    get { statusCode }.isEqualTo(200)
}
val list = response.body!!.toList()
expectThat(list).containsExactly(
    """{"id":"5","name":"userName"}""",
    """{"id":"6","name":"otherUserName"}"""
)

```

### The Low Level API

There is also a really nice low level api if you need more flexibility.

```kotlin
class Reverser : SuspendingHandler {
    override suspend fun handle(request: Request, requestContext: RequestContext): Response {
        return (response(ByteBuffer.wrap(request.withBody().body!!.reversedArray())))
    }
}
//...
Restaurant {
    route(Method.POST, "/handlers/reverser", Reverser())
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
