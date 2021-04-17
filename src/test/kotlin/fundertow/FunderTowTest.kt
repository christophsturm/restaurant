package fundertow

import failfast.describe
import io.undertow.Undertow
import io.undertow.UndertowOptions
import io.undertow.server.handlers.PathHandler
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.net.ServerSocket

class UserService : RestService {
    fun create(user: User): User = User("userId", "userName")
}

data class User(val id: String?, val name: String)


object FunderTowTest {
    @Suppress("BlockingMethodInNonBlockingContext")
    val context = describe(FunderTow::class) {
        val funderTow = autoClose(FunderTow(mapOf("/api/user" to UserService()))) { it.close() }
        val client = okhttp3.OkHttpClient()
        it("returns 404 if the route is not found") {
            val response = client.newCall(
                Request.Builder().url("http://localhost:${funderTow.port}/unconfigured-url")
                    .post("""{"name":"sentName"}""".toRequestBody()).build()
            ).execute()
            expectThat(response) {
                get { code }.isEqualTo(404)
            }
        }
    }
}

class FunderTow(serviceMapping: Map<String, RestService>) : AutoCloseable {
    val port: Int = findFreePort()

    private fun findFreePort(): Int = ServerSocket(0).use {
        it.reuseAddress = true
        it.localPort
    }

    private val undertow: Undertow = Undertow.builder()
        .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
        .addHttpListener(port, "127.0.0.1")
        .setHandler(PathHandler())
        .build()

    init {
        undertow.start()
    }

    override fun close() {
    }

}

interface RestService
