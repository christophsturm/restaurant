package fundertow

import failfast.describe
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

class UserService : RestService {
    fun create(user: User): User = User("userId", "userName")
}

data class User(val id: String?, val name: String)


object FunderTowTest {
    @Suppress("BlockingMethodInNonBlockingContext")
    val context = describe(FunderTow::class) {
        val funderTow = autoClose(
            FunderTow(
                mapOf(
                    "/api/user" to RestWrapper(UserService()),
                    "/handlers/reverser" to ReverserService()
                )
            )
        ) { it.close() }
        val client = okhttp3.OkHttpClient()
        fun request(path: String, config: Request.Builder.() -> Request.Builder = { this }): Response {
            return client.newCall(
                Request.Builder().url("http://localhost:${funderTow.port}$path").config().build()
            ).execute()
        }

        it("returns 404 if the route is not found") {
            val response = request("/unconfigured-url")
            expectThat(response) {
                get { code }.isEqualTo(404)
            }
        }
        it("calls handlers with body and returns result") {
            val response = request("/handlers/reverser") { post("""jakob""".toRequestBody()) }
            expectThat(response) {
                get { code }.isEqualTo(200)
                get { body }.isNotNull().get { string() }.isEqualTo("bokaj")
            }
        }

        describe("rest services") {
            pending("calls create method on post request") {
                val response = request("/api/user") { post("""{"name":"sentName"}""".toRequestBody()) }
                expectThat(response) {
                    get { code }.isEqualTo(200)
                }
            }
        }
    }
}

class ReverserService : HttpService {
    override fun handle(requestBody: ByteArray): ByteArray = requestBody.reversedArray()
}


class RestWrapper(userService: UserService) : HttpService {
    override fun handle(requestBody: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }
}
