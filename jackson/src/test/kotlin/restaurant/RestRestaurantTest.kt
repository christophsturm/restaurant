@file:Suppress("UNUSED_VARIABLE", "BlockingMethodInNonBlockingContext")

package restaurant

import failgood.describe
import org.junit.platform.commons.annotation.Testable
import restaurant.internal.HobbiesService
import restaurant.internal.UsersService
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.single


fun restaurant(serviceMapping: RoutingDSL.() -> Unit) =
    Restaurant(mapper = JacksonMapper(), serviceMapping = serviceMapping)

@Testable
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
                val response = restaurant.request("/handlers/empty")
                expectThat(response) {
                    get { statusCode() }.isEqualTo(HttpStatus.NO_CONTENT_204)
                    get { body() }.isNotNull().isEmpty()
                }
            }

            describe("rest routes") {
                val restaurant = autoClose(
                    restaurant {
                        namespace("/api") {
                            resources(UsersService())
                        }
                    }
                )

                describe("post requests") {
                    val response = restaurant.request("/api/users") { post("""{"name":"userName"}""") }
                    it("returns 201 - Created on successful post request") {
                        expectThat(response).get { statusCode() }.isEqualTo(HttpStatus.CREATED_201)
                    }
                    it("calls create method on post request") {
                        expectThat(response).get { body() }.isEqualTo("""{"id":"userId","name":"userName"}""")
                    }
                }
                it("calls show method on get request with id") {
                    val response = restaurant.request("/api/users/5")
                    expectThat(response) {
                        get { statusCode() }.isEqualTo(200)
                        get { body() }.isEqualTo("""{"id":"5","name":"User 5"}""")
                    }
                }
                it("calls index method on get request without id") {
                    val response = restaurant.request("/api/users")
                    expectThat(response) {
                        get { statusCode() }.isEqualTo(200)
                        get { body() }.isEqualTo("""[{"id":"5","name":"userName"},{"id":"6","name":"userName"}]""")
                    }
                }
                it("calls update method on put request") {
                    val response =
                        restaurant.request("/api/users/5") { put("""{"name":"userName"}""") }
                    expectThat(response) {
                        get { statusCode() }.isEqualTo(200)
                        get { body() }.isEqualTo("""{"id":"5","name":"userName"}""")
                    }
                }
                it("calls delete method on delete request") {
                    val response =
                        restaurant.request("/api/users/5") { delete() }
                    expectThat(response) {
                        get { statusCode() }.isEqualTo(200)
                        get { body() }.isEqualTo("""{"status":"user 5 deleted"}""")
                    }
                }
                it("sets json content type") {
                    val response = restaurant.request("/api/users")
                    expectThat(response).get { headers().allValues(HttpHeader.CONTENT_TYPE) }.single()
                        .isEqualTo(ContentType.APPLICATION_JSON)
                }
                describe("error handling") {
                    describe("malformed requests") {
                        pending("returns a useful error message") {
                            val response =
                                restaurant.request("/api/users") { post("""{"nam":"userName"}""") }
                            expectThat(response).get { statusCode() }.isEqualTo(HttpStatus.BAD_REQUEST_400)
                        }

                    }
                }

            }
            pending("nested routes") {
                val restaurant = autoClose(
                    restaurant {
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

@Suppress("unused")
private fun RoutingDSL.resource(@Suppress("UNUSED_PARAMETER") service: RestService) {
}





