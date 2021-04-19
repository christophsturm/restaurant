package restaurant

import io.undertow.Undertow
import io.undertow.UndertowOptions
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.PathHandler
import io.undertow.util.Methods
import java.net.ServerSocket
import java.nio.ByteBuffer


class Restaurant(serviceMapping: Map<String, HttpService>) : AutoCloseable {
    val port: Int = findFreePort()

    private fun findFreePort(): Int = ServerSocket(0).use {
        it.reuseAddress = true
        it.localPort
    }

    private val undertow: Undertow = run {

        val pathHandler = PathHandler()
        serviceMapping.forEach { (key, value) ->
            pathHandler.addPrefixPath(key, HttpServiceHandler(value))
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
    override fun handleRequest(ex: HttpServerExchange) {
        ex.requestReceiver.receiveFullBytes { exchange, body ->
            if (exchange.requestMethod == Methods.POST)
                exchange.statusCode = 201
            exchange.responseSender.send(ByteBuffer.wrap(service.handle(body)))
        }
    }

}

interface RestService

interface HttpService {
    fun handle(requestBody: ByteArray): ByteArray
}

