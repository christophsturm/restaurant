package restaurant

import failfast.describe
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import restaurant.internal.HobbiesService
import restaurant.internal.UsersService
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull


object RestaurantTest {
    @Suppress("BlockingMethodInNonBlockingContext")
    val context = describe(Restaurant::class) {
        val restaurant = autoClose(
            Restaurant {
                post("/handlers/reverser", ReverserService())
                namespace("/api") {
                    resources(UsersService()) {
                        resources(HobbiesService()) // user has many hobbies
                    }
                    resource(CartService()) // singular resource
                }
            }
        ) { it.close() }
        val client = okhttp3.OkHttpClient()
        fun request(path: String, config: Request.Builder.() -> Request.Builder = { this }): Response {
            return autoClose(
                client.newCall(
                    Request.Builder().url("http://localhost:${restaurant.port}$path").config().build()
                ).execute()
            ) { it.close() }
        }

        it("returns 404 if the route is not found") {
            val response = request("/unconfigured-url")
            expectThat(response).get { code }.isEqualTo(404)
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
                val response = request("/api/users") { post("""{"name":"userName"}""".toRequestBody()) }
                it("returns 201 - Created on successful post request") {
                    expectThat(response).get { code }.isEqualTo(201)
                }
                it("calls create method on post request") {
                    expectThat(response).get { body }.isNotNull().get { string() }
                        .isEqualTo("""{"id":"userId","name":"userName"}""")
                }
            }
            it("calls show method on get request with id") {
                val response = request("/api/users/5")
                expectThat(response) {
                    get { code }.isEqualTo(200)
                    get { body }.isNotNull().get { string() }.isEqualTo("""{"id":"5","name":"User 5"}""")
                }
            }
            it("calls index method on get request without id") {
                val response = request("/api/users")
                expectThat(response) {
                    get { code }.isEqualTo(200)
                    get { body }.isNotNull().get { string() }
                        .isEqualTo("""[{"id":"5","name":"userName"},{"id":"6","name":"userName"}]""")
                }
            }
            it("calls update method on put request") {
                val response = request("/api/users/5") { put("""{"name":"userName"}""".toRequestBody()) }
                expectThat(response) {
                    get { code }.isEqualTo(200)
                    get { body }.isNotNull().get { string() }.isEqualTo("""{"id":"5","name":"userName"}""")
                }
            }

        }
    }
}
/* A singular resource. */
class CartService : RestService {

}


class ReverserService : HttpService {
    override suspend fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray? =
        requestBody?.reversedArray()
}


