package restaurant

import failgood.Test
import failgood.describe
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.nio.ByteBuffer


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
            it("returns 404 if the route is not found") {
                val response = restaurant.request("/unconfigured-url")
                expectThat(response).get { statusCode() }.isEqualTo(HttpStatus.NOT_FOUND_404)
            }
            it("calls handlers with body and returns result") {
                val response =
                    restaurant.request("/handlers/reverser") { post("""jakob""") }
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
                expectThat(restaurant.request("/")) {
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
                expectThat(restaurant.request("/")) {
                    get { statusCode() }.isEqualTo(418)
                    get { body() }.isEqualTo("sorry: error message")
                }
            }
            it("handles errors in the error handler gracefully") {
                val restaurant = Restaurant(exceptionHandler = { _: Throwable ->
                    throw Exception("oops error handler failed")
                }) { route(Method.GET, "/", ExceptionsHandler()) }
                expectThat(restaurant.request("/")) {
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
                expectThat(restaurant.request("/not-found")) {
                    get { statusCode() }.isEqualTo(418)
                    get { body() }.isEqualTo("not found but anyway I'm teapot")
                }
            }
        }
        it("exposes its base url for easier testing") {
            val restaurant = autoClose(Restaurant { })
            expectThat(restaurant.baseUrl).isEqualTo("http://localhost:${restaurant.port}")
        }

    }
}
