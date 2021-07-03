package restaurant

import failgood.describe
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.nio.ByteBuffer


@Testable
class RestaurantTest {
    val context = describe(Restaurant::class) {

        describe("routing") {
            val restaurant = autoClose(
                Restaurant {
                    namespace("/handlers") {
                        route(Method.POST, "reverser") { ex, _ ->
                            response(
                                ByteBuffer.wrap(
                                    ex.readBody().reversedArray()
                                )
                            )
                        }
                    }
                }
            )
            it("returns 404 if the route is not found") {
                val response = req(restaurant, "/unconfigured-url")
                expectThat(response).get { statusCode() }.isEqualTo(404)
            }
            it("calls handlers with body and returns result") {
                val response = request(restaurant, "/handlers/reverser") { post("""jakob""".toRequestBody()) }
                expectThat(response) {
                    get { code }.isEqualTo(200)
                    get { body }.isNotNull().get { string() }.isEqualTo("bokaj")
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
                expectThat(req(restaurant, "/exceptions")) {
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
                expectThat(req(restaurant, "/exceptions")) {
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
                expectThat(req(restaurant, "/not-found")) {
                    get { statusCode() }.isEqualTo(418)
                    get { body() }.isEqualTo("not found but anyway I'm teapot")
                }
            }
        }

    }
}
