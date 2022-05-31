package restaurant.internal.undertow

import io.undertow.io.IoCallback
import io.undertow.io.Sender
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.HttpString
import io.undertow.util.SameThreadExecutor
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import mu.KotlinLogging
import restaurant.ByteBufferResponse
import restaurant.FlowResponse
import restaurant.MutableRequestContext
import restaurant.StatusResponse
import restaurant.StringResponse
import restaurant.SuspendingHandler
import java.io.IOException

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
                val response = suspendHandler.handle(UndertowRequest(exchange), MutableRequestContext())
                exchange.statusCode = response.status
                response.headers.forEach {
                    exchange.responseHeaders.add(HttpString(it.key), it.value)
                }
                when (response) {
                    is ByteBufferResponse -> {
                        exchange.responseSender.send(response.body)
                    }
                    is StatusResponse -> {
                        exchange.endExchange()
                    }
                    is StringResponse -> {
                        exchange.responseSender.send(response.body)
                    }

                    is FlowResponse -> {
                        val responseSender = exchange.responseSender
                        response.body.collect {
                            val deferred = CompletableDeferred<Unit>()
                            responseSender.send(it, object : IoCallback {
                                override fun onComplete(exchange: HttpServerExchange?, sender: Sender?) {
                                    deferred.complete(Unit)
                                }

                                override fun onException(
                                    exchange: HttpServerExchange?,
                                    sender: Sender?,
                                    exception: IOException?
                                ) {
                                    deferred.completeExceptionally(exception!!)
                                }
                            })
                            deferred.await()
                        }
                        responseSender.close()


                    }
                }
            }
        })
    }
}
