package restaurant.internal.undertow

import io.undertow.io.IoCallback
import io.undertow.io.Sender
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.HttpString
import io.undertow.util.SameThreadExecutor
import java.io.IOException
import java.lang.Runnable
import java.nio.ByteBuffer
import kotlinx.coroutines.CompletableDeferred
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
        exchange.dispatch(
            SameThreadExecutor.INSTANCE,
            Runnable {
                requestScope.launch {
                    val response =
                        suspendHandler.handle(UndertowRequest(exchange), MutableRequestContext())
                    try {
                        exchange.statusCode = response.status
                    } catch (e: IllegalStateException) {
                        // setStatusCode throws IllegalStateException when the response is already
                        // started.
                        // we sometimes see this on ci so we are rethrowing it with more info for
                        // now.
                        throw RestaurantException(
                            "Response already started when trying to send response $response," +
                                " body: ${response.bodyString()}",
                            e)
                    }
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
                            exchange.responseSender.let { responseSender ->
                                response.body.collect { responseSender.asyncSend(it) }
                                responseSender.close()
                            }
                        }

                        is ByteArrayFlowResponse -> {
                            exchange.responseSender.let { responseSender ->
                                response.body.collect { responseSender.asyncSend(it) }
                                responseSender.close()
                            }
                        }
                    }
                }
            })
    }

    private suspend fun Sender.asyncSend(it: ByteArray) {
        val deferred = CompletableDeferred<Unit>()
        send(ByteBuffer.wrap(it), CompletingIOCallback(deferred))
        deferred.await()
    }

    private suspend fun Sender.asyncSend(it: String) {
        val deferred = CompletableDeferred<Unit>()
        send(it, CompletingIOCallback(deferred))
        deferred.await()
    }

    /** an IOCallback that completes a CompletableDeferred */
    class CompletingIOCallback(private val deferred: CompletableDeferred<Unit>) : IoCallback {
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
    }
}
