@file:Suppress("unused")

package restaurant.internal

import kotlinx.coroutines.delay
import restaurant.rest.RestService

data class User(val id: String?, val name: String)
data class Hobby(val name: String)
class UserService : RestService {
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

    suspend fun delete(userId: Int): DeleteReply {
        delay(1)
        return DeleteReply("user $userId deleted")
    }
}

class UserStringPKService : RestService {
    suspend fun index(): List<User> {
        delay(1)
        return listOf(User("5", "userName"), User("6", "userName"))
    }

    suspend fun create(user: User): User {
        delay(1)
        return user.copy(id = "userId")
    }

    suspend fun show(userId: String): User {
        delay(1)
        return User(id = userId, name = "User $userId")
    }

    suspend fun update(userId: String, user: User): User {
        delay(1)
        return user.copy(id = userId)
    }

    suspend fun delete(userId: String): DeleteReply {
        delay(1)
        return DeleteReply("user $userId deleted")
    }
}

data class DeleteReply(val status: String)

class NonSuspendUserService : RestService {
    fun index(): List<User> = listOf(User("5", "userName"), User("6", "userName"))
    fun create(user: User): User = user.copy(id = "userId")
    fun show(userId: Int): User = User(id = userId.toString(), name = "User $userId")
    fun update(userId: Int, user: User): User = user.copy(id = userId.toString())
    fun delete(userId: Int): DeleteReply = DeleteReply("user $userId deleted")
}

class NonSuspendStringPKUserService : RestService {
    fun index(): List<User> = listOf(User("5", "userName"), User("6", "userName"))
    fun create(user: User): User = user.copy(id = "userId")
    fun show(userId: String): User = User(id = userId, name = "User $userId")
    fun update(userId: String, user: User): User = user.copy(id = userId)
    fun delete(userId: String): DeleteReply = DeleteReply("user $userId deleted")
}

class HobbyService : RestService {
    suspend fun create(userId: Int, hobby: Hobby): Hobby {
        delay(1)
        return Hobby("user $userId's hobby ${hobby.name}")
    }
}
