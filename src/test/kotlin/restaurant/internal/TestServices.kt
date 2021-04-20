package restaurant.internal

import kotlinx.coroutines.delay
import restaurant.RestService

data class User(val id: String?, val name: String)

class UserService : RestService {
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
