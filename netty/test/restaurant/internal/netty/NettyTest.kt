package restaurant.internal.netty

import failgood.Test
import failgood.testCollection
import kotlin.test.assertNotNull
import restaurant.findFreePort
import restaurant.response

@Test
class NettyTest {
    val test = testCollection {
        it("can create a netty server on a random port") {
            autoClose(buildNetty(emptyList(), { _, _ -> response(200) }, null, "localhost")) {
                it.server.close()
            }
        }
        it("can create a netty server on a fixed port") {
            val port = findFreePort()
            val result =
                autoClose(buildNetty(emptyList(), { _, _ -> response(200) }, port, "localhost")) {
                    it.server.close()
                }
            assert(result.port == port)
        }
        it("fails when the port is already used") {
            val usedPort =
                autoClose(buildNetty(emptyList(), { _, _ -> response(200) }, null, "localhost")) {
                        it.server.close()
                    }
                    .port
            val exception =
                assertNotNull(
                    kotlin
                        .runCatching {
                            buildNetty(
                                emptyList(), { _, _ -> response(200) }, usedPort, "localhost")
                        }
                        .exceptionOrNull())
            assert(exception.message!!.contains("could not start server on port $usedPort"))
        }
    }
}
