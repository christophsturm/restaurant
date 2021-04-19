package restaurant.internal

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import failfast.describe
import kotlinx.coroutines.delay
import restaurant.HttpService
import restaurant.RestService
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.charset.Charset

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

}

object RestServiceHandlerTest {
    val context = describe(RestServiceHandler::class) {
        it("wraps a Rest Service") {
            val handler: HttpService = RestServiceHandler(UserService(), jacksonObjectMapper())
            expectThat(
                String(
                    handler.handle("""{"name":"userName"}""".toByteArray()),
                    Charset.defaultCharset()
                )
            ).isEqualTo("""{"id":"userId","name":"userName"}""")
        }
    }
}
