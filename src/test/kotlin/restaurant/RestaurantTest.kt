package restaurant

import failfast.describe
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import restaurant.internal.UserService
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull


object RestaurantTest {
    @Suppress("BlockingMethodInNonBlockingContext")
    val context = describe(Restaurant::class) {
        val restaurant = autoClose(
            Restaurant {
                post("/handlers/reverser", ReverserService())
                resource("/api/user", UserService())

            }
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
                get { code }.isEqualTo(201)
                get { body }.isNotNull().get { string() }.isEqualTo("bokaj")
            }
        }

        describe("rest services") {
            describe("post requests") {
                val response = request("/api/user") { post("""{"name":"userName"}""".toRequestBody()) }
                it("returns 201 - Created on successful post request") {
                    expectThat(response).get { code }.isEqualTo(201)
                }
                it("calls create method on post request") {
                    expectThat(response).get { body }.isNotNull().get { string() }
                        .isEqualTo("""{"id":"userId","name":"userName"}""")
                }
            }
            pending("calls show method on get request") {
                val response = request("/api/user/5")
                expectThat(response) {
                    get { code }.isEqualTo(200)
                    get { body }.isNotNull().get { string() }.isEqualTo("""{"id":"5","name":"userName"}""")
                }
            }
        }
    }
}

class ReverserService : HttpService {
    override fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray? =
        requestBody?.reversedArray()
}


