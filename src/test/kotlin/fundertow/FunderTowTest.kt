package fundertow

import failfast.describe
import io.undertow.Undertow
import io.undertow.UndertowOptions
import okhttp3.Request
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.net.ServerSocket

object FunderTowTest {
    @Suppress("BlockingMethodInNonBlockingContext")
    val context = describe(FunderTow::class) {
        it("exists") {
            val funderTow = autoClose(FunderTow()) { it.close() }
            val client = okhttp3.OkHttpClient()
            val response = client.newCall(
                Request.Builder().url("http://localhost:${funderTow.port}/path").build()
            ).execute()
            expectThat(response) {
                get { code }.isEqualTo(500) // it seems undertow returns 500 without a routing config
            }
        }
    }
}

class FunderTow : AutoCloseable {
    val port: Int = findFreePort()

    private fun findFreePort(): Int = ServerSocket(0).use {
        it.reuseAddress = true
        it.localPort
    }

    private val undertow: Undertow = Undertow.builder()
        .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
        .addHttpListener(port, "127.0.0.1")
        .build()

    init {
        undertow.start()
    }

    override fun close() {
    }

}
