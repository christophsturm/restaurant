package restaurant.internal

import restaurant.ContentType
import restaurant.HttpHeader
import restaurant.HttpStatus
import restaurant.Method
import restaurant.Request
import restaurant.RequestContext
import restaurant.Response
import restaurant.RestService
import restaurant.Route
import restaurant.SuspendingHandler
import restaurant.response
import java.nio.ByteBuffer
import kotlin.reflect.KFunction
import kotlin.reflect.full.functions

class RoutesAdder(private val objectMapper: Mapper) {
    @OptIn(ExperimentalStdlibApi::class)
    fun routesFor(restService: RestService, path: String): List<Route> {
        val functions = restService::class.functions.associateBy { it.name }
        return buildList {
            functions["create"]?.let {
                add(
                    Route(
                        Method.POST, path,
                        PostRestServiceHandler(
                            objectMapper,
                            RestFunction(it, restService)
                        )
                    )
                )
            }

            functions["show"]?.let {
                add(
                    Route(
                        Method.GET, "$path/{id}",
                        GetRestServiceHandler(
                            objectMapper,
                            RestFunction(it, restService)
                        )
                    )
                )
            }

            functions["index"]?.let {
                add(
                    Route(
                        Method.GET,
                        path,
                        GetListRestServiceHandler(objectMapper, RestFunction(it, restService))
                    )
                )
            }

            functions["update"]?.let {
                add(
                    Route(
                        Method.PUT, "$path/{id}",
                        PutRestServiceHandler(
                            objectMapper,
                            RestFunction(it, restService)
                        )
                    )
                )
            }

            functions["delete"]?.let<KFunction<*>, Unit> {
                add(
                    Route(
                        Method.DELETE, "$path/{id}",
                        GetRestServiceHandler(
                            objectMapper,
                            RestFunction(it, restService)
                        )
                    )
                )
            }
        }
    }
}

val contentTypeJson = mapOf(HttpHeader.CONTENT_TYPE to ContentType.APPLICATION_JSON)


@OptIn(ExperimentalStdlibApi::class)
private class PutRestServiceHandler(
    private val objectMapper: Mapper,
    val function: RestFunction
) : SuspendingHandler {
    val payloadType = function.payloadType!!

    override suspend fun handle(request: Request, requestContext: RequestContext): Response {
        val id = request.queryParameters.let {
            it["id"]?.singleOrNull()
                ?: throw RuntimeException("id variable not found. variables: ${it.keys.joinToString()}")
        }
        val payload = objectMapper.readValue(request.readBody(), payloadType)
        val result = function.callSuspend(payload, id, requestContext)
        return response(objectMapper.writeValueAsBytes(result))
    }
}

@Suppress("CanBeParameter")
@OptIn(ExperimentalStdlibApi::class)
private class PostRestServiceHandler(
    private val objectMapper: Mapper,
    val restFunction: RestFunction
) : SuspendingHandler {
    private val payloadType = restFunction.payloadType!!

    override suspend fun handle(request: Request, requestContext: RequestContext): Response {
        val payload = objectMapper.readValue(request.readBody(), payloadType)
        val result = restFunction.callSuspend(payload, null, requestContext)
        return response(
            HttpStatus.CREATED_201,
            ByteBuffer.wrap(objectMapper.writeValueAsBytes(result)),
            contentTypeJson
        )
    }
}

private class GetRestServiceHandler(
    private val objectMapper: Mapper,
    val function: RestFunction
) : SuspendingHandler {
    override suspend fun handle(request: Request, requestContext: RequestContext): Response {
        val id = request.queryParameters.let {
            it["id"]?.singleOrNull()
                ?: throw RuntimeException("id variable not found. variables: ${it.keys.joinToString()}")
        }

        return response(objectMapper.writeValueAsBytes(function.callSuspend(null, id, requestContext)), contentTypeJson)
    }
}

@OptIn(ExperimentalStdlibApi::class)
private class GetListRestServiceHandler(
    private val objectMapper: Mapper, val function: RestFunction
) : SuspendingHandler {
    override suspend fun handle(request: Request, requestContext: RequestContext): Response {
        val result = function.callSuspend(null, null, requestContext)
        return if (result == null)
            response(HttpStatus.NO_CONTENT_204)
        else {
            response(objectMapper.writeValueAsBytes(result), contentTypeJson)
        }
    }
}

