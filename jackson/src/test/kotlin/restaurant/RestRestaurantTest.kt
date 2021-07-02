@file:Suppress("UNUSED_VARIABLE", "BlockingMethodInNonBlockingContext")

package restaurant

import failgood.describe
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.platform.commons.annotation.Testable
import restaurant.internal.HobbiesService
import restaurant.internal.UsersService
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.nio.ByteBuffer

class ReverserService : SuspendingHandler {
    override suspend fun handle(exchange: Exchange, requestContext: RequestContext): Response {
        return (response(ByteBuffer.wrap(exchange.readBody().reversedArray())))
    }
}

fun restaurant(serviceMapping: RoutingDSL.() -> Unit) = Restaurant(mapper = JacksonMapper(), serviceMapping = serviceMapping)

@Testable
class RestRestaurantTest {
    val context = describe(Restaurant::class) {
        describe("rest services")
        {
            it("empty responses return 204")
            {
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
                val response = request(restaurant, "/handlers/empty")
                expectThat(response) {
                    get { code }.isEqualTo(204)
                    get { body }.isNotNull().get { string() }.isEmpty()
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
                    val response = request(restaurant, "/api/users") { post("""{"name":"userName"}""".toRequestBody()) }
                    it("returns 201 - Created on successful post request") {
                        println(restaurant.routes.joinToString("\n"))
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
                it("calls delete method on delete request") {
                    val response =
                        request(restaurant, "/api/users/5") { delete("""{"name":"userName"}""".toRequestBody()) }
                    expectThat(response) {
                        get { code }.isEqualTo(200)
                        get { body }.isNotNull().get { string() }.isEqualTo("""{"status":"user 5 deleted"}""")
                    }
                }
                it("sets json content type") {
                    val response = request(restaurant, "/api/users")
                    expectThat(response).get { header("Content-Type") }.isEqualTo("application/json")
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





