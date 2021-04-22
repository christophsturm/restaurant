### Restaurant - Rest without boilerplate

The name will probably change because everybody except me thinks it is stupid.

```kotlin

Restaurant {
    namespace("/api") {
        resources(UsersService())
    }
}

data class User(val id: String?, val name: String)
class UsersService : RestService {
    suspend fun create(user: User): User {
        delay(1)
        return user.copy(id = "userId")
    }

    suspend fun show(userId: Int): User {
        delay(1)
        return User(id = userId.toString(), name = "User $userId")
    }

    suspend fun update(userId: Int, user: User): User {
        delay(1)
        return user.copy(id = userId.toString())
    }

}


describe("rest services") {
    describe("post requests") {
        val response = request("/api/users") { post("""{"name":"userName"}""".toRequestBody()) }
        it("returns 201 - Created on successful post request") {
            expectThat(response).get { code }.isEqualTo(201)
        }
        it("calls create method on post request") {
            expectThat(response).get { body }.isNotNull().get { string() }
                .isEqualTo("""{"id":"userId","name":"userName"}""")
        }
    }
    it("calls show method on get request") {
        val response = request("/api/users/5")
        expectThat(response) {
            get { code }.isEqualTo(200)
            get { body }.isNotNull().get { string() }.isEqualTo("""{"id":"5","name":"User 5"}""")
        }
    }
    it("calls update method on put request") {
        val response = request("/api/users/5") { put("""{"name":"userName"}""".toRequestBody()) }
        expectThat(response) {
            get { code }.isEqualTo(200)
            get { body }.isNotNull().get { string() }.isEqualTo("""{"id":"5","name":"userName"}""")
        }
    }
}

```

Next: nested resources:

```kotlin
namespace("/api") {
    resources(UsersService()) {
        resources(HobbiesService()) // user has many hobbies
    }
}
```

will generate routes like `GET /api/users/10/hobbies/20` or `POST /api/users/10/hobbies`
