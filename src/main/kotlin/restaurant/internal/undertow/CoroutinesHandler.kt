package restaurant.internal.undertow

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
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
                when (val response = suspendHandler.handle(UndertowExchange(exchange), MutableRequestContext())) {
                    is ByteBufferResponse -> {
                        exchange.statusCode = response.status
                        exchange.responseSender.send(response.result)
                    }
                    is StatusResponse -> {
                        exchange.statusCode = response.status
                        exchange.endExchange()
                    }
                    is StringResponse -> {
                        exchange.statusCode = response.status
                        exchange.responseSender.send(response.result)
                    }
                }
            }
        })
    }
}
