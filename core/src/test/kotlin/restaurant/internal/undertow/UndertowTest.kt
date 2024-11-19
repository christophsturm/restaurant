package restaurant.internal.undertow

import failgood.Test
import failgood.tests
import restaurant.response
import kotlin.test.assertNotNull

@Test
class UndertowTest {
    val test = tests {
        it("can create an undertow server") {
            autoClose(buildUndertow(emptyList(), { _, _ -> response(200) }, null, "localhost"), { it.undertow.stop() })
        }
        it("fails when the port is already used") {
            val usedPort = autoClose(
                buildUndertow(emptyList(), { _, _ -> response(200) }, null, "localhost"),
                { it.undertow.stop() }).port
            val exception = assertNotNull(kotlin.runCatching {
                buildUndertow(emptyList(), { _, _ -> response(200) }, usedPort, "localhost")
            }.exceptionOrNull())
            assert(exception.message!!.contains("could not start server on port $usedPort"))
        }
    }
}
