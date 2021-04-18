package fundertow

import failfast.describe
import fundertow.internal.RestServiceHandler
import fundertow.internal.UserService
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull




object FunderTowTest {
    @Suppress("BlockingMethodInNonBlockingContext")
    val context = describe(FunderTow::class) {
        val funderTow = autoClose(
            FunderTow(
                mapOf(
                    "/api/user" to RestServiceHandler(UserService()),
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
            it("calls create method on post request") {
                val response = request("/api/user") { post("""{"name":"sentName"}""".toRequestBody()) }
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


