package restaurant.internal

import com.fasterxml.jackson.databind.ObjectMapper
import restaurant.HttpService
import restaurant.RestService
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.functions
import kotlin.reflect.javaType

class RoutesAdder(private val objectMapper: ObjectMapper) {
    @OptIn(ExperimentalStdlibApi::class)
    fun routesFor(restService: RestService, path: String): List<Route> {
        val functions = restService::class.functions.associateBy { it.name }
        val list = buildList {
            val post = functions["create"]?.let {
                add(Route(Method.POST, path, RestServiceHandler(restService, objectMapper, it)))
            }

            val get = functions["show"]?.let {
                add(Route(Method.GET, "$path/{id}", GetRestServiceHandler(restService, objectMapper, it)))
            }

            val getList = functions["index"]?.let {
                add(Route(Method.GET, path, GetListRestServiceHandler(restService, objectMapper, it)))
            }

            val put = functions["update"]?.let {
                add(Route(Method.PUT, "$path/{id}", PutRestServiceHandler(restService, objectMapper, it)))
            }
        }
        return list
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
    private val service: RestService, private val objectMapper: ObjectMapper, private val function: KFunction<*>
) : HttpService {
    private val parameterType = function.parameters[2].type.javaType as Class<*>

    override suspend fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray {
        val id = pathVariables["id"]?.toInt()
            ?: throw RuntimeException("id variable not found. variables: ${pathVariables.keys.joinToString()}")
        val parameter = objectMapper.readValue(requestBody, parameterType)
        val result = function.callSuspend(service, id, parameter)
        return objectMapper.writeValueAsBytes(result)
    }
}

@OptIn(ExperimentalStdlibApi::class)
private class RestServiceHandler(
    private val service: RestService, private val objectMapper: ObjectMapper, private val function: KFunction<*>
) : HttpService {
    private val parameterType = function.parameters[1].type.javaType as Class<*>

    override suspend fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray {
        val parameter = objectMapper.readValue(requestBody, parameterType)
        val result = function.callSuspend(service, parameter)
        return objectMapper.writeValueAsBytes(result)
    }
}

@OptIn(ExperimentalStdlibApi::class)
private class GetRestServiceHandler(
    private val service: RestService, private val objectMapper: ObjectMapper, private val function: KFunction<*>
) : HttpService {
    override suspend fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray {
        val id = pathVariables["id"]
            ?: throw RuntimeException("id variable not found. variables: ${pathVariables.keys.joinToString()}")
        val parameter = id.toInt()
        return objectMapper.writeValueAsBytes(function.callSuspend(service, parameter))
    }
}

@OptIn(ExperimentalStdlibApi::class)
private class GetListRestServiceHandler(
    private val service: RestService, private val objectMapper: ObjectMapper, private val function: KFunction<*>
) : HttpService {
    override suspend fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray {
        return objectMapper.writeValueAsBytes(function.callSuspend(service))
    }
}
