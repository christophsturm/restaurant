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

data class User(val id: String?, val name: String)
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

## coming next:

### error handling

#### Exception based error handling

Exceptions thrown by the service handler will be converted to a http error reply via a lambda

```kotlin
val restaurant = Restaurant(errorHandler = { ex ->
    ErrorReply(
        status = 500,
        body = InternalServerError("sorry", ex.stackTraceToString())
    )
}/*.....*/)
```

### nested resources:

```kotlin
namespace("/api") {
    resources(UsersService()) {
        resources(HobbiesService()) // user has many hobbies
    }
}
```

will generate routes like `GET /api/users/10/hobbies/20` or `POST /api/users/10/hobbies`

### Authentication via JWT

### Developer friendly

* Show a list of defined routes when a 404 error occurs in development mode.
* Friendly error messages when json can be serialized or deserialized.
* Any stacktrace in undertow or restaurant code that does not show a useful error message should be reported as a bug.
