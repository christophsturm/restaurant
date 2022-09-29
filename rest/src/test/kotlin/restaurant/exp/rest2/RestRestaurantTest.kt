@file:Suppress("UNUSED_VARIABLE", "BlockingMethodInNonBlockingContext", "unused")

package restaurant.exp.rest2

import failgood.Test
import failgood.describe
import restaurant.ContentType
import restaurant.HttpHeader
import restaurant.HttpStatus
import restaurant.Restaurant
import restaurant.RoutingDSL
import restaurant.internal.UsersService
import restaurant.request
import restaurant.response
import restaurant.rest.RestService
import restaurant.rest.path
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.single
import kotlin.reflect.KClass

/*
this is just an experiment for a new rest dsl. probably not interesting for anyone except me right now (CS)
 */
fun restaurant(serviceMapping: RoutingDSL.() -> Unit) = Restaurant(serviceMapping = serviceMapping)

@Test
class RestRestaurantTest {
    val context = describe(Restaurant::class, disabled = true) {
        describe("rest services") {
            it("empty responses return 204") {
                class EmptyReplyService {
                    fun index(): String? {
                        return null
                    }
                }

                val restaurant = autoClose(
                    restaurant {
                        resources(EmptyReplyService(), "/handlers/empty").mapping(index = { index() })
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
                            resources(UsersService()).mapping(
                                index = { index() },
                                create = { create(it.get()) },
                                update = { update(it.get(), it.get()) }
                            )
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
                        it("returns a useful error message") {
                            val requestBody = """{"nam":"userName"}"""
                            val response = restaurant.request("/api/users") { post(requestBody) }
                            expectThat(response) {
                                get { statusCode() }.isEqualTo(HttpStatus.BAD_REQUEST_400)
                                get { body }.isNotNull().contains(requestBody)
                            }
                        }
                    }
                }
            }
            describe("error handling") {
                class ExceptionsService : RestService {
                    fun index() {
                        throw RuntimeException("error message")
                    }
                }
                it("calls error handler with the correct exception") {
                    val restaurant = Restaurant(exceptionHandler = { ex: Throwable ->
                        response(
                            status = 418,
                            result = "sorry: " + ex.message
                        )
                    }) { resources(ExceptionsService()) }
                    expectThat(restaurant.request("/exceptions")) {
                        get { statusCode() }.isEqualTo(418)
                        get { body() }.isEqualTo("sorry: error message")
                    }
                }
            }
        }
    }
}

@Suppress("UnusedReceiverParameter")
private fun <T : Any> RoutingDSL.resources(service: T, path: String = path(service)) = ResourceMapper(service, path)
@Suppress("UNUSED_PARAMETER")
class Context {
    fun <T : Any> get(kClass: KClass<T>): T {
        TODO()
    }
}

private inline fun <reified T : Any> Context.get(): T {
    return this.get(T::class)
}
@Suppress("UNUSED_PARAMETER")
class ResourceMapper<T : Any>(service: T, path: String = path(service)) {
    fun mapping(
        index: suspend T.(Context) -> Unit = {},
        create: suspend T.(Context) -> Unit = {},
        update: suspend T.(Context) -> Unit = {},
        delete: suspend T.(Context) -> Unit = {}
    ) {
    }
}

@Suppress("unused", "UnusedReceiverParameter")
private fun RoutingDSL.resource(@Suppress("UNUSED_PARAMETER") service: RestService) {
}
