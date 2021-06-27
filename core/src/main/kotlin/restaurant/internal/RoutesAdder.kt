package restaurant.internal

import restaurant.Method
import restaurant.RequestContext
import restaurant.RestService
import kotlin.reflect.KFunction
import kotlin.reflect.full.functions

class RoutesAdder(private val objectMapper: Mapper) {
    @OptIn(ExperimentalStdlibApi::class)
    fun routesFor(restService: RestService, path: String): List<RestRoute> {
        val functions = restService::class.functions.associateBy { it.name }
        return buildList {
            functions["create"]?.let {
                add(
                    RestRoute(
                        Method.POST, path, PostRestServiceHandler(
                            objectMapper,
                            RestFunction(it, restService)
                        )
                    )
                )
            }

            functions["show"]?.let {
                add(
                    RestRoute(
                        Method.GET, "$path/{id}", GetRestServiceHandler(
                            objectMapper,
                            RestFunction(it, restService)
                        )
                    )
                )
            }

            functions["index"]?.let {
                add(RestRoute(Method.GET, path, GetListRestServiceHandler(objectMapper, RestFunction(it, restService))))
            }

            functions["update"]?.let {
                add(
                    RestRoute(
                        Method.PUT, "$path/{id}", PutRestServiceHandler(
                            objectMapper,
                            RestFunction(it, restService)
                        )
                    )
                )
            }

            functions["delete"]?.let<KFunction<*>, Unit> {
                add(
                    RestRoute(
                        Method.DELETE, "$path/{id}", GetRestServiceHandler(
                            objectMapper,
                            RestFunction(it, restService)
                        )
                    )
                )
            }
        }
    }
}


data class RestRoute(val method: Method, val path: String, val httpService: HttpService)

@OptIn(ExperimentalStdlibApi::class)
private class PutRestServiceHandler(
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
        val payload = objectMapper.readValue(requestBody, function.payloadType!!)
        val result = function.callSuspend(payload, id, requestContext)
        return objectMapper.writeValueAsBytes(result)
    }
}

@Suppress("CanBeParameter")
@OptIn(ExperimentalStdlibApi::class)
private class PostRestServiceHandler(
    private val objectMapper: Mapper,
    val restFunction: RestFunction
) : HttpService {
    private val payloadType = restFunction.payloadType

    override suspend fun handle(
        requestBody: ByteArray?,
        pathVariables: Map<String, String>,
        requestContext: RequestContext
    ): ByteArray {
        val payload = objectMapper.readValue(requestBody, payloadType!!)
        val result = restFunction.callSuspend(payload, null, requestContext)
        return objectMapper.writeValueAsBytes(result)
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

