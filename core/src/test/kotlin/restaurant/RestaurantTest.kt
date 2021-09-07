package restaurant

import failgood.Test
import failgood.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo
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
                expectThat(response).get { statusCode() }.isEqualTo(404)
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
            class ExceptionsService : RestService {
                fun index() {
                    throw RuntimeException("error message")
                }
            }
            it("returns status 500 per default on error") {
                val restaurant = autoClose(Restaurant { resources(ExceptionsService()) })
                expectThat(restaurant.request("/exceptions")) {
                    get { statusCode() }.isEqualTo(500)
                    get { body() }.isEqualTo("internal server error")
                }

            }
            it("calls error handler to create error reply") {
                val restaurant = Restaurant(exceptionHandler = { ex: Throwable ->
                    response(
                        status = 418,
                        result = "sorry: " + ex.cause!!.message
                    )
                }) { resources(ExceptionsService()) }
                expectThat(restaurant.request("/exceptions")) {
                    get { statusCode() }.isEqualTo(418)
                    get { body() }.isEqualTo("sorry: error message")
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

    }
}
