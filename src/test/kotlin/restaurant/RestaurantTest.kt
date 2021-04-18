package restaurant

import failfast.describe
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import restaurant.internal.RestServiceHandler
import restaurant.internal.UserService
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull


object restaurantTest {
    @Suppress("BlockingMethodInNonBlockingContext")
    val context = describe(Restaurant::class) {
        val restaurant = autoClose(
            Restaurant(
                mapOf(
                    "/api/user" to RestServiceHandler(UserService()),
                    "/handlers/reverser" to ReverserService()
                )
            )
        ) { it.close() }
        val client = okhttp3.OkHttpClient()
        fun request(path: String, config: Request.Builder.() -> Request.Builder = { this }): Response {
            return client.newCall(
                Request.Builder().url("http://localhost:${restaurant.port}$path").config().build()
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
            it("calls create method on post request") {
                val response = request("/api/user") { post("""{"name":"userName"}""".toRequestBody()) }
                expectThat(response) {
                    get { code }.isEqualTo(200)
                    get { body }.isNotNull().get { string() }.isEqualTo("""{"id":"userId","name":"userName"}""")
                }
            }
        }
    }
}

class ReverserService : HttpService {
    override fun handle(requestBody: ByteArray): ByteArray = requestBody.reversedArray()
}


