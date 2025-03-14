package restaurant.rest.internal

import kotlin.reflect.KFunction
import kotlin.reflect.full.functions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import restaurant.*
import restaurant.internal.Mapper
import restaurant.rest.RestService

class RoutesAdder(private val objectMapper: Mapper) {
    fun routesFor(restService: RestService, path: String): List<Route> {
        val functions = restService::class.functions.associateBy { it.name }
        return buildList {
            functions["create"]?.let {
                add(
                    Route(
                        Method.POST,
                        path,
                        PostRestServiceHandler(objectMapper, RestFunction(it, restService))))
            }

            functions["show"]?.let {
                add(
                    Route(
                        Method.GET,
                        "$path/{id}",
                        GetRestServiceHandler(objectMapper, RestFunction(it, restService))))
            }

            functions["index"]?.let {
                add(
                    Route(
                        Method.GET,
                        path,
                        GetListRestServiceHandler(objectMapper, RestFunction(it, restService))))
            }

            functions["update"]?.let {
                add(
                    Route(
                        Method.PUT,
                        "$path/{id}",
                        PutRestServiceHandler(objectMapper, RestFunction(it, restService))))
            }

            functions["delete"]?.let<KFunction<*>, Unit> {
                add(
                    Route(
                        Method.DELETE,
                        "$path/{id}",
                        GetRestServiceHandler(objectMapper, RestFunction(it, restService))))
            }
        }
    }
}

val contentTypeJson = mapOf(HttpHeader.CONTENT_TYPE to ContentType.APPLICATION_JSON)

private class PutRestServiceHandler(private val objectMapper: Mapper, val function: RestFunction) :
    SuspendingHandler {
    val payloadType = function.payloadType!!

    override suspend fun handle(request: Request, requestContext: MutableRequestContext): Response {
        val id =
            request.queryParameters.let {
                it["id"]?.singleOrNull()
                    ?: throw RuntimeException(
                        "id variable not found. variables: ${it.keys.joinToString()}")
            }
        val payload = request.withBody().body?.let { objectMapper.readValue(it, payloadType) }
        val result = function.callSuspend(payload, id, requestContext)
        return objectMapper.responseOrNull(result)
    }
}

private class PostRestServiceHandler(
    private val objectMapper: Mapper,
    val restFunction: RestFunction
) : SuspendingHandler {
    private val payloadType = restFunction.payloadType!!

    override suspend fun handle(request: Request, requestContext: MutableRequestContext): Response {
        val payload = request.withBody().body?.let { objectMapper.readValue(it, payloadType) }
        val result = restFunction.callSuspend(payload, null, requestContext)
        return objectMapper.responseOrNull(result, HttpStatus.CREATED_201)
    }
}

private class GetRestServiceHandler(private val objectMapper: Mapper, val function: RestFunction) :
    SuspendingHandler {
    override suspend fun handle(request: Request, requestContext: MutableRequestContext): Response {
        val id =
            request.queryParameters.let {
                it["id"]?.singleOrNull()
                    ?: throw RuntimeException(
                        "id variable not found. variables: ${it.keys.joinToString()}")
            }

        return objectMapper.responseOrNull(function.callSuspend(null, id, requestContext))
    }
}

private class GetListRestServiceHandler(
    private val objectMapper: Mapper,
    val function: RestFunction
) : SuspendingHandler {
    override suspend fun handle(request: Request, requestContext: MutableRequestContext): Response {
        val result = function.callSuspend(null, null, requestContext)
        return objectMapper.responseOrNull(result)
    }
}

private fun Mapper.responseOrNull(result: Any?, statusCode: Int = HttpStatus.OK_200): Response {
    return if (result == null) {
        response(HttpStatus.NO_CONTENT_204)
    } else {
        if (result is Flow<*>) {
            FlowResponse(
                mapOf(),
                statusCode,
                flow {
                    result.collect {
                        if (it != null) {
                            emit(writeValueAsString(it))
                            emit("\n")
                        }
                    }
                })
        } else {
            response(statusCode, writeValueAsBytes(result), contentTypeJson)
        }
    }
}
