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
        return buildList {
            functions["create"]?.let {
                add(Route(Method.POST, path, CreateRestServiceHandler(restService, objectMapper, it)))
            }

            functions["show"]?.let {
                add(Route(Method.GET, "$path/{id}", GetRestServiceHandler(restService, objectMapper, it)))
            }

            functions["index"]?.let {
                add(Route(Method.GET, path, GetListRestServiceHandler(restService, objectMapper, it)))
            }

            functions["update"]?.let {
                add(Route(Method.PUT, "$path/{id}", PutRestServiceHandler(restService, objectMapper, it)))
            }

            functions["delete"]?.let<KFunction<*>, Unit> {
                add(Route(Method.DELETE, "$path/{id}", GetRestServiceHandler(restService, objectMapper, it)))
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

@Suppress("CanBeParameter")
@OptIn(ExperimentalStdlibApi::class)
private class CreateRestServiceHandler(
    private val service: RestService, private val objectMapper: ObjectMapper, private val function: KFunction<*>
) : HttpService {
    private val func = RestFunction(function, service)
    private val parameterType = func.parameterType

    override suspend fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray {
        val parameter = objectMapper.readValue(requestBody, parameterType)
        val result = func.callSuspend(parameter, null)
        return objectMapper.writeValueAsBytes(result)
    }
}

private class GetRestServiceHandler(
    private val service: RestService, private val objectMapper: ObjectMapper, private val function: KFunction<*>
) : HttpService {
    private val func = RestFunction(function, service)
    override suspend fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray {
        val id = pathVariables["id"]
            ?: throw RuntimeException("id variable not found. variables: ${pathVariables.keys.joinToString()}")
        return objectMapper.writeValueAsBytes(func.callSuspend(null, id))
    }
}

@OptIn(ExperimentalStdlibApi::class)
private class GetListRestServiceHandler(
    service: RestService, private val objectMapper: ObjectMapper, function: KFunction<*>
) : HttpService {
    val func = RestFunction(function, service)
    override suspend fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray {
        return objectMapper.writeValueAsBytes(func.callSuspend(null, null))
    }
}

