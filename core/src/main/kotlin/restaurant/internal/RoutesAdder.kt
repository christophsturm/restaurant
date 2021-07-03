package restaurant.internal

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
                        Method.GET, "$path/{id}", HttpServiceHandler(
                            GetRestServiceHandler(
                                objectMapper,
                                RestFunction(it, restService)
                            ), false, HttpStatus.OK_200
                        )
                    )
                )
            }

            functions["index"]?.let {
                add(
                    Route(
                        Method.GET,
                        path,
                        HttpServiceHandler(
                            GetListRestServiceHandler(objectMapper, RestFunction(it, restService)),
                            false,
                            HttpStatus.OK_200
                        )
                    )
                )
            }

            functions["update"]?.let {
                add(
                    Route(
                        Method.PUT, "$path/{id}", HttpServiceHandler(
                            PutRestServiceHandler(
                                objectMapper,
                                RestFunction(it, restService)
                            ), true, HttpStatus.OK_200
                        )
                    ))
            }

            functions["delete"]?.let<KFunction<*>, Unit> {
                add(
                    Route(
                        Method.DELETE, "$path/{id}", HttpServiceHandler(
                            GetRestServiceHandler(
                                objectMapper,
                                RestFunction(it, restService)
                            ), false, HttpStatus.OK_200
                        )
                    )
                )
            }
        }
    }
}


@OptIn(ExperimentalStdlibApi::class)
private class PutRestServiceHandler(
    private val objectMapper: Mapper,
    val function: RestFunction
) : HttpService {
    val payloadType = function.payloadType!!

    override suspend fun handle(
        requestBody: ByteArray?,
        pathVariables: Map<String, String>,
        requestContext: RequestContext
    ): ByteArray {
        val id = pathVariables["id"]
            ?: throw RuntimeException("id variable not found. variables: ${pathVariables.keys.joinToString()}")
        val payload = objectMapper.readValue(requestBody, payloadType)
        val result = function.callSuspend(payload, id, requestContext)
        return objectMapper.writeValueAsBytes(result)
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
        return response(HttpStatus.CREATED_201, ByteBuffer.wrap(objectMapper.writeValueAsBytes(result)))
    }
}

private class GetRestServiceHandler(
    private val objectMapper: Mapper,
    val function: RestFunction
) : HttpService {
    override suspend fun handle(
        requestBody: ByteArray?,
        pathVariables: Map<String, String>,
        requestContext: RequestContext
    ): ByteArray {
        val id = pathVariables["id"]
            ?: throw RuntimeException("id variable not found. variables: ${pathVariables.keys.joinToString()}")
        return objectMapper.writeValueAsBytes(function.callSuspend(null, id, requestContext))
    }
}

@OptIn(ExperimentalStdlibApi::class)
private class GetListRestServiceHandler(
    private val objectMapper: Mapper, val function: RestFunction
) : HttpService {
    override suspend fun handle(
        requestBody: ByteArray?,
        pathVariables: Map<String, String>,
        requestContext: RequestContext
    ): ByteArray? {
        val result = function.callSuspend(null, null, requestContext)
        return result?.let { objectMapper.writeValueAsBytes(it) }
    }
}

