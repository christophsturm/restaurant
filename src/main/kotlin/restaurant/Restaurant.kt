package restaurant

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.undertow.Undertow
import io.undertow.UndertowOptions
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.RoutingHandler
import io.undertow.server.handlers.error.SimpleErrorPageHandler
import restaurant.internal.RoutesAdder
import java.net.ServerSocket
import java.nio.ByteBuffer

internal fun findFreePort(): Int = ServerSocket(0).use {
    it.reuseAddress = true
    it.localPort
}

class Restaurant(val port: Int = findFreePort(), serviceMapping: RoutingDSL.() -> Unit) : AutoCloseable {

    private val objectMapper = jacksonObjectMapper()

    private val undertow: Undertow = run {

        val routingHandler = RoutingHandler()
        RoutingDSL(routingHandler, RoutesAdder(objectMapper)).serviceMapping()
        Undertow.builder()
            .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
            .addHttpListener(port, "127.0.0.1")
            .setHandler(SimpleErrorPageHandler(routingHandler))
            .build()
    }

    init {
        undertow.start()
    }

    override fun close() {
    }

}

@RestDSL
class RoutingDSL(
    private val routingHandler: RoutingHandler,
    private val routesAdder: RoutesAdder,
    val prefix: String = ""
) {
    fun post(path: String, service: HttpService) {
        routingHandler.post(path, HttpServiceHandler(service, 201))
    }

    fun resources(path: String, service: RestService, function: ResourceDSL.() -> Unit = {}) {
        val resolvedPath = prefix + path
        val routes = routesAdder.routesFor(service)
        routes.post?.let { routingHandler.post(resolvedPath, HttpServiceHandler(it, 201)) }
        routes.get?.let { routingHandler.get("$resolvedPath/{id}", NoBodyServiceHandler(it)) }
        routes.put?.let { routingHandler.put("$resolvedPath/{id}", HttpServiceHandler(it, 200)) }
        ResourceDSL(resolvedPath).function()
    }

    private fun path(service: RestService) = service::class.simpleName!!.toLowerCase().removeSuffix("service")
    fun namespace(prefix: String, function: RoutingDSL.() -> Unit) {
        RoutingDSL(routingHandler, routesAdder, this.prefix + prefix).function()
    }

    fun resources(service: RestService, function: ResourceDSL.() -> Unit = {}) {
        resources("/${path(service)}", service, function)
    }
}

@DslMarker
annotation class RestDSL

@RestDSL
class ResourceDSL(resolvedPath: String) {
    fun resources(service: RestService, function: ResourceDSL.() -> Unit = {}) {
    }
}

private fun call(exchange: HttpServerExchange, service: HttpService, requestBody: ByteArray?) {
    exchange.responseSender.send(
        ByteBuffer.wrap(
            service.handle(
                requestBody,
                exchange.queryParameters.mapValues { it.value.single() })
        )
    )
}

class NoBodyServiceHandler(private val service: HttpService) : HttpHandler {
    override fun handleRequest(exchange: HttpServerExchange) = call(exchange, service, null)
}

class HttpServiceHandler(private val service: HttpService, private val statusCode: Int) : HttpHandler {
    override fun handleRequest(ex: HttpServerExchange) {
        ex.requestReceiver.receiveFullBytes { exchange, body ->
            exchange.statusCode = statusCode
            call(exchange, service, body)
        }
    }

}

interface RestService

interface HttpService {
    fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray?
}

