package fundertow

import io.undertow.Undertow
import io.undertow.UndertowOptions
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.PathHandler
import java.net.ServerSocket
import java.nio.ByteBuffer


class FunderTow(serviceMapping: Map<String, HttpService>) : AutoCloseable {
    val port: Int = findFreePort()

    private fun findFreePort(): Int = ServerSocket(0).use {
        it.reuseAddress = true
        it.localPort
    }

    private val undertow: Undertow = run {

        val pathHandler = PathHandler()
        serviceMapping.forEach { (key, value) ->
            pathHandler.addExactPath(key, HttpServiceHandler(value))
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

class HttpServiceHandler(private val service: HttpService) : HttpHandler {
    override fun handleRequest(exchange: HttpServerExchange) {
        exchange.requestReceiver.receiveFullBytes { _, message ->
            exchange.responseSender.send(ByteBuffer.wrap(service.handle(message)))
        }


    }

}

interface RestService

interface HttpService {
    fun handle(requestBody: ByteArray): ByteArray
}

