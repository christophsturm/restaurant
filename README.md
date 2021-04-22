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
