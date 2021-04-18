package fundertow.internal

import failfast.describe
import fundertow.HttpService
import fundertow.RestService
import kotlinx.coroutines.delay
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.charset.Charset

data class User(val id: String?, val name: String)

class UserService : RestService {
    suspend fun create(user: User): User {
        delay(1)
        return User("userId", "userName")
    }
}

object RestServiceHandlerTest {
    val context = describe(RestServiceHandler::class) {
        it("wraps a Rest Service") {
            val handler: HttpService = RestServiceHandler(UserService())
            expectThat(
                String(
                    handler.handle("""{"name":"sentName"}""".toByteArray()),
                    Charset.defaultCharset()
                )
            ).isEqualTo("""{"id":"userId","name":"userName"}""")
        }
    }
}
