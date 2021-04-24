package restaurant.internal

import kotlinx.coroutines.delay
import restaurant.RestService

data class User(val id: String?, val name: String)
data class Hobby(val name: String)
class UsersService : RestService {
    suspend fun index(): List<User> {
        delay(1)
        return listOf(User("5", "userName"), User("6", "userName"))
    }

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

class HobbiesService : RestService {
    suspend fun create(userId: Int, hobby: Hobby): Hobby {
        delay(1)
        return Hobby("user $userId's hobby ${hobby.name}")
    }
}
