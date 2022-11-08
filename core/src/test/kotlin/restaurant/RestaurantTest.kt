package restaurant

import failgood.Test
import failgood.describe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isNotNull
import strikt.assertions.size
import java.nio.ByteBuffer
import kotlin.test.assertNotNull

@Test
class RestaurantTest {
    val context = describe(Restaurant::class) {

        describe("routing") {
            val restaurant = autoClose(
                Restaurant {
                    namespace("/handlers") {
                        route(Method.POST, "reverser") { ex, _ ->
                            response(
                                ByteBuffer.wrap(
                                    ex.withBody().body!!.reversedArray()
                                )
                            )
                        }
                    }
                }
            )
            it("exposes routes") {
                with(assertNotNull(restaurant.routes.single())) {
                    assert(method == Method.POST)
                    assert(path == "/handlers/reverser")
                    assert(this.wrappers.isEmpty())
                }
            }
            it("returns 404 if the route is not found") {
                val response = restaurant.sendRequest("/unconfigured-url")
                expectThat(response).get { statusCode() }.isEqualTo(HttpStatus.NOT_FOUND_404)
            }
            it("calls handlers with body and returns result") {
                val response =
                    restaurant.sendRequest("/handlers/reverser") { post("""jakob""") }
                expectThat(response) {
                    get { statusCode() }.isEqualTo(200)
                    get { body() }.isEqualTo("bokaj")
                }
            }
        }
        describe("error handling") {
            class ExceptionsHandler : SuspendingHandler {
                override suspend fun handle(request: Request, requestContext: RequestContext): Response {
                    throw RuntimeException("error message")
                }
            }
            it("returns status 500 per default on error") {
                val restaurant = autoClose(Restaurant { route(Method.GET, "/", ExceptionsHandler()) })
                expectThat(restaurant.sendRequest("/")) {
                    get { statusCode() }.isEqualTo(500)
                    get { body }.isNotNull().contains("internal server error")
                }
            }
            it("calls error handler to create error reply") {
                val restaurant = Restaurant(exceptionHandler = { ex: Throwable ->
                    response(
                        status = 418,
                        result = "sorry: " + ex.message
                    )
                }) { route(Method.GET, "/", ExceptionsHandler()) }
                expectThat(restaurant.sendRequest("/")) {
                    get { statusCode() }.isEqualTo(418)
                    get { body() }.isEqualTo("sorry: error message")
                }
            }
            it("handles errors in the error handler gracefully") {
                val restaurant = Restaurant(exceptionHandler = {
                    throw Exception("oops error handler failed")
                }) { route(Method.GET, "/", ExceptionsHandler()) }
                expectThat(restaurant.sendRequest("/")) {
                    get { statusCode() }.isEqualTo(500)
                    get { body }.isNotNull().contains("error in error handler").contains("oops error handler failed")
                }
            }
            it("calls default handler if no suitable route is found") {
                val restaurant =
                    Restaurant(defaultHandler = { _: Request, _: RequestContext ->
                        response(
                            418,
                            "not found but anyway I'm teapot"
                        )
                    }) { }
                expectThat(restaurant.sendRequest("/not-found")) {
                    get { statusCode() }.isEqualTo(418)
                    get { body() }.isEqualTo("not found but anyway I'm teapot")
                }
            }
        }
        it("exposes its base url for easier testing") {
            val port = findFreePort()
            val restaurant = autoClose(Restaurant(host = "0.0.0.0", port = port) { })
            expectThat(restaurant.baseUrl).isEqualTo("http://0.0.0.0:$port")
        }
        it("can be called with null as port for autodetect") {
            val restaurant = autoClose(Restaurant(port = null) { })
            assert(restaurant.sendRequest("/").statusCode == HttpStatus.NOT_FOUND_404)
        }
        describe("async roundtrip") {
            it("works") {
                var stopFlow = false
                val californiaStreaming = flow {
                    while (!stopFlow) {
                        this.emit("california\n")
                        delay(10)
                    }
                }

                val restaurant = autoClose(
                    Restaurant {
                        route(Method.GET, "/async") { _, _ ->
                            FlowResponse(mapOf(), 200, californiaStreaming)
                        }
                    }
                )
                val response = restaurant.sendStreamingRequest("/async")
                // the flow is still sending, but  we already got our answer. we must be streaming!
                expectThat(response).get { statusCode }.isEqualTo(200)
                var received = 0
                // let it send 10 elements to and then quit
                expectThat(
                    response.body!!.map {
                        if (received++ > 10) {
                            stopFlow = true
                        }
                        it
                    }.toList()
                ) {
                    size.isGreaterThan(10)
                    all {
                        isEqualTo("california")
                    }
                }
            }
        }
    }
}
