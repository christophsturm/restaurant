package restaurant.internal.undertow

import failgood.Test
import failgood.tests
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import restaurant.findFreePort
import restaurant.response

@Test
class UndertowTest {
    val test = tests {
        it("can create an undertow server on a random port") {
            autoClose(buildUndertow(emptyList(), { _, _ -> response(200) }, null, "localhost")) {
                it.undertow.stop()
            }
        }
        it("can create an undertow server on a fixed port") {
            val port = findFreePort()
            val result =
                autoClose(
                    buildUndertow(emptyList(), { _, _ -> response(200) }, port, "localhost")) {
                        it.undertow.stop()
                    }
            assert(result.port == port)
        }
        it("fails when the port is already used") {
            val usedPort =
                autoClose(
                        buildUndertow(emptyList(), { _, _ -> response(200) }, null, "localhost")) {
                            it.undertow.stop()
                        }
                    .port
            val exception =
                assertNotNull(
                    kotlin
                        .runCatching {
                            buildUndertow(
                                emptyList(), { _, _ -> response(200) }, usedPort, "localhost")
                        }
                        .exceptionOrNull())
            assert(exception.message!!.contains("could not start server on port $usedPort"))
        }
        it("retries automatic port selection 3 times") {
            val usedPort =
                autoClose(
                        buildUndertow(emptyList(), { _, _ -> response(200) }, null, "localhost")) {
                            it.undertow.stop()
                        }
                    .port
            val exception =
                assertNotNull(
                    kotlin
                        .runCatching {
                            buildUndertow(
                                emptyList(),
                                { _, _ -> response(200) },
                                null,
                                "localhost",
                                { usedPort })
                        }
                        .exceptionOrNull())
            assertEquals(
                exception.message!!,
                "could not start restaurant after trying 3 times. ports tried: [$usedPort, $usedPort, $usedPort]")
        }
    }
}
