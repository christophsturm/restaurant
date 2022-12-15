package restaurant.rest.internal

import failgood.Test
import failgood.describe
import failgood.mock.mock
import kotlinx.coroutines.delay
import restaurant.rest.RestService

/**
 * look at RoutesAdderFunctionalTest.kt for the real test
 */
@Test
class RoutesAdderTest {
    val context = describe<RoutesAdder> {
        it("does not crash when called") {

            val routesAdder = RoutesAdder(mock())
            val service = UserService()
            val rootPath = "root"
            routesAdder.routesFor(service, rootPath)
        }
    }
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
}
data class User(val id: String?, val name: String)
data class DeleteReply(val status: String)
