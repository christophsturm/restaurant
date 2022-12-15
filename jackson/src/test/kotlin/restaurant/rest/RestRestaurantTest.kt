@file:Suppress("UNUSED_VARIABLE", "BlockingMethodInNonBlockingContext", "unused")

package restaurant.rest

import failgood.Ignored
import failgood.Test
import failgood.describe
import restaurant.ContentType
import restaurant.HttpHeader
import restaurant.HttpStatus
import restaurant.JacksonMapper
import restaurant.Restaurant
import restaurant.RoutingDSL
import restaurant.internal.HobbyService
import restaurant.internal.UserService
import restaurant.response
import restaurant.sendRequest
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.single

fun restaurant(serviceMapping: RoutingDSL.() -> Unit) =
    Restaurant(mapper = JacksonMapper(), serviceMapping = serviceMapping)

/**
 * this tests the rest support with jackson, not only the jackson support
 */
@Test
class RestRestaurantTest {
    val context = describe(Restaurant::class) {
        describe("rest services") {
            it("empty responses return 204") {
                class EmptyReplyService : RestService {
                    fun index(): String? {
                        return null
                    }
                }

                val restaurant = autoClose(
                    restaurant {
                        resources(EmptyReplyService(), "/handlers/empty")
                    }
                )
                val response = restaurant.sendRequest("/handlers/empty")
                expectThat(response) {
                    get { statusCode() }.isEqualTo(HttpStatus.NO_CONTENT_204)
                    get { body() }.isNotNull().isEmpty()
                }
            }

            describe("rest routes") {
                val restaurant = autoClose(
                    restaurant {
                        namespace("/api") {
                            resources(UserService())
                        }
                    }
                )

                describe("post requests") {
                    val response = restaurant.sendRequest("/api/users") { post("""{"name":"userName"}""") }
                    it("returns 201 - Created on successful post request") {
                        expectThat(response).get { statusCode() }.isEqualTo(HttpStatus.CREATED_201)
                    }
                    it("calls create method on post request") {
                        expectThat(response).get { body() }.isEqualTo("""{"id":"userId","name":"userName"}""")
                    }
                }
                it("calls show method on get request with id") {
                    val response = restaurant.sendRequest("/api/users/5")
                    expectThat(response) {
                        get { statusCode() }.isEqualTo(200)
                        get { body() }.isEqualTo("""{"id":"5","name":"User 5"}""")
                    }
                }
                it("calls index method on get request without id") {
                    val response = restaurant.sendRequest("/api/users")
                    expectThat(response) {
                        get { statusCode() }.isEqualTo(200)
                        get { body() }.isEqualTo("""[{"id":"5","name":"userName"},{"id":"6","name":"userName"}]""")
                    }
                }
                it("calls update method on put request") {
                    val response =
                        restaurant.sendRequest("/api/users/5") { put("""{"name":"userName"}""") }
                    expectThat(response) {
                        get { statusCode() }.isEqualTo(200)
                        get { body() }.isEqualTo("""{"id":"5","name":"userName"}""")
                    }
                }
                it("calls delete method on delete request") {
                    val response =
                        restaurant.sendRequest("/api/users/5") { delete() }
                    expectThat(response) {
                        get { statusCode() }.isEqualTo(200)
                        get { body() }.isEqualTo("""{"status":"user 5 deleted"}""")
                    }
                }
                it("sets json content type") {
                    val response = restaurant.sendRequest("/api/users")
                    expectThat(response).get { headers().allValues(HttpHeader.CONTENT_TYPE) }.single()
                        .isEqualTo(ContentType.APPLICATION_JSON)
                }
                describe("error handling") {
                    describe("malformed requests") {
                        it("returns a useful error message") {
                            val requestBody = """{"nam":"userName"}"""
                            val response = restaurant.sendRequest("/api/users") { post(requestBody) }
                            expectThat(response) {
                                get { statusCode() }.isEqualTo(HttpStatus.BAD_REQUEST_400)
                                get { body }.isNotNull().contains(requestBody)
                            }
                        }
                    }
                }
            }
            describe("error handling") {
                class ExceptionService : RestService {
                    fun index() {
                        throw RuntimeException("error message")
                    }
                }
                it("calls error handler with the correct exception") {
                    val restaurant = Restaurant(mapper = JacksonMapper(), exceptionHandler = { ex: Throwable ->
                        response(
                            status = 418,
                            result = "sorry: " + ex.message
                        )
                    }) { resources(ExceptionService()) }
                    expectThat(restaurant.sendRequest("/exceptions")) {
                        get { statusCode() }.isEqualTo(418)
                        get { body() }.isEqualTo("sorry: error message")
                    }
                }
            }
            describe("planned features") {

                it("does not yet support nested routes", ignored = Ignored.Because("not yet implemented")) {
                    val restaurant = autoClose(
                        restaurant {
                            namespace("/api") {
                                resources(UserService()) {
                                    resources(HobbyService()) // user has many hobbies
                                }
                            }
                        }
                    )
                }
                it("does not yet support singular routes", ignored = Ignored.Because("not yet implemented")) {
                    class CartService : RestService

                    val restaurant = autoClose(
                        restaurant {
                            namespace("/api") {
                                resource(CartService()) // singular resource
                            }
                        }
                    )
                }
            }
        }
    }
}

@Suppress("unused", "UnusedReceiverParameter")
private fun RoutingDSL.resource(@Suppress("UNUSED_PARAMETER") service: RestService) {
}
