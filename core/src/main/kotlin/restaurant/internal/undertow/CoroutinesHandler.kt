package restaurant.internal.undertow

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.HttpString
import io.undertow.util.SameThreadExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mu.KotlinLogging
import restaurant.*

private val logger = KotlinLogging.logger {}

class CoroutinesHandler(private val suspendHandler: SuspendingHandler) : HttpHandler {
    override fun handleRequest(exchange: HttpServerExchange) {
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
                val response = suspendHandler.handle(UndertowExchange(exchange), MutableRequestContext())
                exchange.statusCode = response.status
                response.headers.forEach {
                    exchange.responseHeaders.add(HttpString(it.key), it.value)
                }
                when (response) {
                    is ByteBufferResponse -> {
                        exchange.responseSender.send(response.result)
                    }
                    is StatusResponse -> {
                        exchange.endExchange()
                    }
                    is StringResponse -> {
                        exchange.responseSender.send(response.result)
                    }
                }
            }
        })
    }
}
