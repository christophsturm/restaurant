package restaurant.internal

import restaurant.Exchange
import restaurant.RequestContext
import restaurant.Response
import restaurant.SuspendingHandler
import restaurant.response
import java.nio.ByteBuffer

interface HttpService {
    suspend fun handle(
        requestBody: ByteArray?,
        pathVariables: Map<String, String>,
        requestContext: RequestContext
    ): ByteArray?
}

internal class HttpServiceHandler(
    private val service: HttpService,
    private val readBody: Boolean,
    private val statusCode: Int
) :
    SuspendingHandler {
    override suspend fun handle(exchange: Exchange, requestContext: RequestContext): Response {
        val body = if (readBody) exchange.readBody() else null
        val response = service.handle(body, exchange.queryParameters.mapValues { it.value.single() }, requestContext)
        return if (response == null) {
            response(204)
        } else
            response(statusCode, ByteBuffer.wrap(response), mapOf("Content-Type" to "application/json"))
    }

}
