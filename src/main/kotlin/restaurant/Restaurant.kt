package restaurant

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.undertow.Undertow
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.server.RoutingHandler
import io.undertow.server.handlers.error.SimpleErrorPageHandler
import io.undertow.util.SameThreadExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mu.KotlinLogging
import restaurant.internal.RoutesAdder
import java.net.ServerSocket
import java.nio.ByteBuffer

internal fun findFreePort(): Int = ServerSocket(0).use {
    it.reuseAddress = true
    it.localPort
}

private val logger = KotlinLogging.logger {}

data class ErrorReply(val status: Int, val body: String)

typealias ThrowableToErrorReply = (Throwable) -> ErrorReply

val defaultErrorHandler: ThrowableToErrorReply = { ErrorReply(500, "internal server error") }

class Restaurant(
    val port: Int = findFreePort(),
    val errorHandler: ThrowableToErrorReply = defaultErrorHandler,
    private val objectMapper: ObjectMapper = jacksonObjectMapper(),
    serviceMapping: RoutingDSL.() -> Unit,
) : AutoCloseable {

    private val undertow: Undertow = run {

        val routingHandler = RoutingHandler()
        RoutingDSL(routingHandler, RoutesAdder(objectMapper), errorHandler = errorHandler).serviceMapping()
        Undertow.builder()
//            .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
            .addHttpListener(port, "127.0.0.1")
            .setHandler(SimpleErrorPageHandler(routingHandler))
            .build()
    }

    init {
        undertow.start()
    }

    override fun close() {
        undertow.stop()
    }

}

@RestDSL
class RoutingDSL(
    private val routingHandler: RoutingHandler,
    private val routesAdder: RoutesAdder,
    private val prefix: String = "",
    private val errorHandler: ThrowableToErrorReply
) {
    fun post(path: String, service: HttpService) {
        routingHandler.post(path, HttpServiceHandler(service, 201, errorHandler))
    }

    fun resources(path: String, service: RestService, function: ResourceDSL.() -> Unit = {}) {
        val resolvedPath = prefix + path
        val routes = routesAdder.routesFor(service)
        routes.post?.let { routingHandler.post(resolvedPath, HttpServiceHandler(it, 201, errorHandler)) }
        routes.get?.let { routingHandler.get("$resolvedPath/{id}", NoBodyServiceHandler(it, errorHandler)) }
        routes.getList?.let { routingHandler.get(resolvedPath, NoBodyServiceHandler(it, errorHandler)) }
        routes.put?.let { routingHandler.put("$resolvedPath/{id}", HttpServiceHandler(it, 200, errorHandler)) }
        ResourceDSL(resolvedPath).function()
    }

    private fun path(service: RestService) = service::class.simpleName!!.toLowerCase().removeSuffix("service")
    fun namespace(prefix: String, function: RoutingDSL.() -> Unit) {
        RoutingDSL(routingHandler, routesAdder, this.prefix + prefix, errorHandler).function()
    }

    fun resources(service: RestService, function: ResourceDSL.() -> Unit = {}) {
        resources("/${path(service)}", service, function)
    }

    fun resource(service: RestService) {

    }
}

@DslMarker
annotation class RestDSL

@RestDSL
class ResourceDSL(resolvedPath: String) {
    fun resources(service: RestService, function: ResourceDSL.() -> Unit = {}) {
    }
}

private fun callSuspend(
    exchange: HttpServerExchange,
    service: HttpService,
    requestBody: ByteArray?,
    errorHandler: ThrowableToErrorReply
) {
    val requestScope = CoroutineScope(Dispatchers.Unconfined)
    exchange.addExchangeCompleteListener { _, nextListener ->
        try {
            requestScope.cancel()
        } catch (e: Exception) {
            logger.error(e) { "error closing coroutine context" }
        }
        nextListener.proceed()
    }
    exchange.dispatch(SameThreadExecutor.INSTANCE, Runnable {
        requestScope.launch {
            try {
                val result =
                    ByteBuffer.wrap(service.handle(requestBody, exchange.queryParameters.mapValues { it.value.single() }))
                exchange.responseSender.send(result)
            } catch (e: Exception) {
                val result = errorHandler(e)
                exchange.statusCode = result.status
                exchange.responseSender.send(result.body)
            }
        }
    })
}

private class NoBodyServiceHandler(private val service: HttpService, private val errorHandler: ThrowableToErrorReply) : HttpHandler {
    override fun handleRequest(exchange: HttpServerExchange) = callSuspend(exchange, service, null, errorHandler)
}

private class HttpServiceHandler(
    private val service: HttpService,
    private val statusCode: Int,
    private val errorHandler: ThrowableToErrorReply
) : HttpHandler {
    override fun handleRequest(ex: HttpServerExchange) {
        ex.requestReceiver.receiveFullBytes { exchange, body ->
            exchange.statusCode = statusCode
                callSuspend(exchange, service, body, errorHandler)
        }
    }
}

interface RestService

interface HttpService {
    suspend fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray?
}

