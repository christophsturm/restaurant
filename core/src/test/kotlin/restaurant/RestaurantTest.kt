package restaurant

import failgood.describe
import okhttp3.RequestBody.Companion.toRequestBody
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.nio.ByteBuffer

class ReverserService : SuspendingHandler {
    override suspend fun handle(exchange: Exchange, requestContext: RequestContext): Response {
        return (response(ByteBuffer.wrap(exchange.readBody().reversedArray())))
    }
}

fun restaurant(serviceMapping: RoutingDSL.() -> Unit) =
    Restaurant(serviceMapping = serviceMapping)

class RestaurantTest {
    val context = describe(Restaurant::class) {

        describe("routing") {
            val restaurant = autoClose(
                restaurant {
                    namespace("/handlers") {
                        route(Method.POST, "reverser", ReverserService())
                    }
                }
            )
            it("returns 404 if the route is not found") {
                val response = request(restaurant, "/unconfigured-url")
                expectThat(response).get { code }.isEqualTo(404)
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
                val restaurant = autoClose(restaurant { resources(ExceptionsService()) })
                expectThat(request(restaurant, "/exceptions")) {
                    get { code }.isEqualTo(500)
                    get { body }.isNotNull().get { string() }.isEqualTo("internal server error")
                }

            }
            it("calls error handler to create error reply") {
                val restaurant = Restaurant(exceptionHandler = { ex: Throwable ->
                    response(
                        status = 418,
                        result = "sorry: " + ex.cause!!.message
                    )
                }) { resources(ExceptionsService()) }
                expectThat(request(restaurant, "/exceptions")) {
                    get { code }.isEqualTo(418)
                    get { body }.isNotNull().get { string() }.isEqualTo("sorry: error message")
                }
            }
            it("calls default handler if no suitable route is found") {
                val restaurant =
                    Restaurant(defaultHandler = { _: Exchange, _: RequestContext ->
                        response(
                            418,
                            "not found but anyway I'm teapot"
                        )
                    }) { }
                expectThat(request(restaurant, "/not-found")) {
                    get { code }.isEqualTo(418)
                    get { body }.isNotNull().get { string() }.isEqualTo("not found but anyway I'm teapot")
                }
            }
        }

    }
}
