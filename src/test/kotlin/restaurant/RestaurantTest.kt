@file:Suppress("UNUSED_VARIABLE", "BlockingMethodInNonBlockingContext")

package restaurant

import failfast.describe
import okhttp3.RequestBody.Companion.toRequestBody
import restaurant.internal.HobbiesService
import restaurant.internal.UsersService
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

object RestaurantTest {
    @Suppress()
    val context = describe(Restaurant::class) {
        describe("routing") {
            class ReverserService : HttpService {
                override suspend fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray? =
                    requestBody?.reversedArray()
            }

            val restaurant = autoClose(
                Restaurant {
                    post("/handlers/reverser", ReverserService())
                }
            )
            it("returns 404 if the route is not found") {
                val response = request(restaurant, "/unconfigured-url")
                expectThat(response).get { code }.isEqualTo(404)
            }
            it("calls handlers with body and returns result") {
                val response = request(restaurant, "/handlers/reverser") { post("""jakob""".toRequestBody()) }
                expectThat(response) {
                    get { code }.isEqualTo(201)
                    get { body }.isNotNull().get { string() }.isEqualTo("bokaj")
                }
            }
        }

        describe("rest services") {
            describe("rest routes") {
                val restaurant = autoClose(
                    Restaurant {
                        namespace("/api") {
                            resources(UsersService())
                        }
                    }
                )

                describe("post requests") {
                    val response = request(restaurant, "/api/users") { post("""{"name":"userName"}""".toRequestBody()) }
                    it("returns 201 - Created on successful post request") {
                        expectThat(response).get { code }.isEqualTo(201)
                    }
                    it("calls create method on post request") {
                        expectThat(response).get { body }.isNotNull().get { string() }
                            .isEqualTo("""{"id":"userId","name":"userName"}""")
                    }
                }
                it("calls show method on get request with id") {
                    val response = request(restaurant, "/api/users/5")
                    expectThat(response) {
                        get { code }.isEqualTo(200)
                        get { body }.isNotNull().get { string() }.isEqualTo("""{"id":"5","name":"User 5"}""")
                    }
                }
                it("calls index method on get request without id") {
                    val response = request(restaurant, "/api/users")
                    expectThat(response) {
                        get { code }.isEqualTo(200)
                        get { body }.isNotNull().get { string() }
                            .isEqualTo("""[{"id":"5","name":"userName"},{"id":"6","name":"userName"}]""")
                    }
                }
                it("calls update method on put request") {
                    val response =
                        request(restaurant, "/api/users/5") { put("""{"name":"userName"}""".toRequestBody()) }
                    expectThat(response) {
                        get { code }.isEqualTo(200)
                        get { body }.isNotNull().get { string() }.isEqualTo("""{"id":"5","name":"userName"}""")
                    }
                }
            }
            describe("error handling") {
                class ExceptionsService : RestService {
                    fun index() {
                        throw RuntimeException("error message")
                    }
                }
                it("returns status 500 per default on error") {
                    val restaurant = autoClose(Restaurant { resources(ExceptionsService()) })
                    expectThat(request(restaurant, "/exceptions")) {
                        get{code}.isEqualTo(500)
                        get{body}.isNotNull().get{string()}.isEqualTo("internal server error")
                    }

                }
                it("calls error handler to create error reply") {
                    val restaurant = Restaurant(errorHandler = { ex ->
                        ErrorReply(
                            status = 418,
                            body = "sorry: " + ex.cause!!.message
                        )
                    }) { resources(ExceptionsService()) }
                    expectThat(request(restaurant, "/exceptions")) {
                        get{code}.isEqualTo(418)
                        get{body}.isNotNull().get{string()}.isEqualTo("sorry: error message")
                    }

                }
            }
            pending("nested routes") {
                val restaurant = autoClose(
                    Restaurant {
                        namespace("/api") {
                            resources(UsersService()) {
                                resources(HobbiesService()) // user has many hobbies
                            }
                        }
                    }
                )

            }
            pending("singular routes") {
                class CartService : RestService

                val restaurant = autoClose(
                    Restaurant {
                        namespace("/api") {
                            resource(CartService()) // singular resource
                        }
                    }
                )
            }
        }
    }
}





