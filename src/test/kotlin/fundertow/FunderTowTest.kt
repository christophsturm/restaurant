package fundertow

import failfast.describe
import okhttp3.Request
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.net.ServerSocket

object FunderTowTest {
    val context = describe(FunderTow::class) {
        pending("exists") {
            val funderTow = autoClose(FunderTow()) { it.close() }
            val client = okhttp3.OkHttpClient()
            val response = client.newCall(
                Request.Builder().url("http://localhost:${funderTow.port}/path").build()
            ).execute()
            expectThat(response) {
                get { code }.isEqualTo(404)
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

    override fun close() {
        TODO("Not yet implemented")
    }

}
