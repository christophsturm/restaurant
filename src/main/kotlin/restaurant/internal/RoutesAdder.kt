package restaurant.internal

import com.fasterxml.jackson.databind.ObjectMapper
import restaurant.HttpService
import restaurant.RestService
import kotlin.reflect.KFunction
import kotlin.reflect.full.functions

class RoutesAdder(private val objectMapper: ObjectMapper) {
    @OptIn(ExperimentalStdlibApi::class)
    fun routesFor(restService: RestService, path: String): List<Route> {
        val functions = restService::class.functions.associateBy { it.name }
        return buildList {
            functions["create"]?.let {
                add(
                    Route(
                        Method.POST, path, CreateRestServiceHandler(
                            objectMapper,
                            RestFunction(it, restService)
                        )
                    )
                )
            }

            functions["show"]?.let {
                add(
                    Route(
                        Method.GET, "$path/{id}", GetRestServiceHandler(
                            objectMapper,
                            RestFunction(it, restService)
                        )
                    )
                )
            }

            functions["index"]?.let {
                add(Route(Method.GET, path, GetListRestServiceHandler(objectMapper, RestFunction(it, restService))))
            }

            functions["update"]?.let {
                add(
                    Route(
                        Method.PUT, "$path/{id}", PutRestServiceHandler(
                            objectMapper,
                            RestFunction(it, restService)
                        )
                    )
                )
            }

            functions["delete"]?.let<KFunction<*>, Unit> {
                add(
                    Route(
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

enum class Method {
    GET,
    PUT,
    POST,
    DELETE
}

data class Route(val method: Method, val path: String, val handler: HttpService)

@OptIn(ExperimentalStdlibApi::class)
private class PutRestServiceHandler(
    private val objectMapper: ObjectMapper,
    val function: RestFunction
) : HttpService {

    override suspend fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray {
        val id = pathVariables["id"]
            ?: throw RuntimeException("id variable not found. variables: ${pathVariables.keys.joinToString()}")
        val parameter = objectMapper.readValue(requestBody, function.parameterType)
        val result = function.callSuspend(parameter, id)
        return objectMapper.writeValueAsBytes(result)
    }
}

@Suppress("CanBeParameter")
@OptIn(ExperimentalStdlibApi::class)
private class CreateRestServiceHandler(
    private val objectMapper: ObjectMapper,
    val restFunction: RestFunction
) : HttpService {
    private val parameterType = restFunction.parameterType

    override suspend fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray {
        val parameter = objectMapper.readValue(requestBody, parameterType)
        val result = restFunction.callSuspend(parameter, null)
        return objectMapper.writeValueAsBytes(result)
    }
}

private class GetRestServiceHandler(
    private val objectMapper: ObjectMapper,
    val function: RestFunction
) : HttpService {
    override suspend fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray {
        val id = pathVariables["id"]
            ?: throw RuntimeException("id variable not found. variables: ${pathVariables.keys.joinToString()}")
        return objectMapper.writeValueAsBytes(function.callSuspend(null, id))
    }
}

@OptIn(ExperimentalStdlibApi::class)
private class GetListRestServiceHandler(
    private val objectMapper: ObjectMapper, val function: RestFunction
) : HttpService {
    override suspend fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray {
        return objectMapper.writeValueAsBytes(function.callSuspend(null, null))
    }
}

