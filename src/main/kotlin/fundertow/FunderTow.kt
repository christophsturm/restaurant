package fundertow

import io.undertow.Undertow
import io.undertow.UndertowOptions
import io.undertow.server.handlers.PathHandler
import java.net.ServerSocket

class FunderTow(serviceMapping: Map<String, RestService>) : AutoCloseable {
    val port: Int = findFreePort()

    private fun findFreePort(): Int = ServerSocket(0).use {
        it.reuseAddress = true
        it.localPort
    }

    private val undertow: Undertow = run {

        val pathHandler = PathHandler()
        serviceMapping.forEach { (key, value) ->
            pathHandler.addExactPath(key, RestServiceHandler(value))
        }
        Undertow.builder()
            .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
            .addHttpListener(port, "127.0.0.1")
            .setHandler(pathHandler)
            .build()
    }

    init {
        undertow.start()
    }

    override fun close() {
    }

}

interface RestService
