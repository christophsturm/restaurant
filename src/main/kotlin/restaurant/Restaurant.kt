package restaurant

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.undertow.Undertow
import io.undertow.UndertowOptions
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.RoutingHandler
import io.undertow.util.Methods
import restaurant.internal.RestServiceHandler
import restaurant.internal.RoutesAdder
import java.net.ServerSocket
import java.nio.ByteBuffer


class Restaurant(serviceMapping: RoutingDSL.() -> Unit) : AutoCloseable {
    val port: Int = findFreePort()

    private fun findFreePort(): Int = ServerSocket(0).use {
        it.reuseAddress = true
        it.localPort
    }

    private val objectMapper = jacksonObjectMapper()

    private val undertow: Undertow = run {

        val routingHandler = RoutingHandler()
        RoutingDSL(routingHandler, objectMapper).serviceMapping()
        Undertow.builder()
            .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
            .addHttpListener(port, "127.0.0.1")
            .setHandler(routingHandler)
            .build()
    }

    init {
        undertow.start()
    }

    override fun close() {
    }

}

class RoutingDSL(private val routingHandler: RoutingHandler, private val objectMapper: ObjectMapper) {
    val routesAdder = RoutesAdder(objectMapper)
    fun post(path: String, service: HttpService) {
        routingHandler.post(path, HttpServiceHandler(service))
    }

    fun resource(path: String, service: RestService) {
        routingHandler.post(
            path,
            HttpServiceHandler(RestServiceHandler(service, objectMapper))
        )
        routingHandler.get(
            "$path/{id}",
            HttpServiceHandler(RestServiceHandler(service, objectMapper))
        )
    }

    private fun path(service: RestService) = service::class.simpleName!!.toLowerCase().removeSuffix("service")
}

class HttpServiceHandler(private val service: HttpService) : HttpHandler {
    override fun handleRequest(ex: HttpServerExchange) {
        ex.requestReceiver.receiveFullBytes { exchange, body ->
            if (exchange.requestMethod == Methods.POST)
                exchange.statusCode = 201
            exchange.responseSender.send(
                ByteBuffer.wrap(
                    service.handle(
                        body,
                        exchange.pathParameters.mapValues { it.value.single() })
                )
            )
        }
    }

}

interface RestService

interface HttpService {
    fun handle(requestBody: ByteArray, pathVariables: Map<String, String>): ByteArray
}

