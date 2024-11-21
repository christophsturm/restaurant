package restaurant

import failgood.Test
import failgood.testsAbout
import java.nio.ByteBuffer
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

@Test
class WrappersTest {
    val context =
        testsAbout("Wrapper Support") {
            val events = mutableListOf<String>()
            val inner = Wrapper { wrapped ->
                SuspendingHandler { request, requestContext ->
                    events.add("inner")
                    wrapped.handle(request, requestContext)
                }
            }
            val outer = Wrapper { wrapped ->
                SuspendingHandler { request, requestContext ->
                    events.add("outer")
                    wrapped.handle(request, requestContext)
                }
            }
            val restaurant =
                autoClose(
                    Restaurant {
                        wrap(outer) {
                            wrap(inner) {
                                route(Method.POST, "/handlers/reverser") { exchange, _ ->
                                    response(
                                        ByteBuffer.wrap(exchange.withBody().body!!.reversedArray()))
                                }
                            }
                        }
                    })
            val response = restaurant.sendRequest("/handlers/reverser") { post("jakob") }
            it("calls the wrapped handler") {
                expectThat(response) {
                    get { statusCode() }.isEqualTo(200)
                    get { body() }.isEqualTo("bokaj")
                }
            }
            it("calls the wrappers") { expectThat(events).containsExactly("outer", "inner") }
        }
}
