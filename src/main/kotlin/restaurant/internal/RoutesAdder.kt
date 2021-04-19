package restaurant.internal

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import restaurant.HttpService
import restaurant.RestService
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.functions
import kotlin.reflect.javaType

class RoutesAdder(private val objectMapper: ObjectMapper) {
    fun routesFor(restService: RestService): Routes {
        val functions = restService::class.functions.associateBy { it.name }
        val post = functions["create"]?.let {
            RestServiceHandler(restService, objectMapper, it)
        }
        val get = functions["show"]?.let {
            GetRestServiceHandler(restService, objectMapper, it)
        }
        return Routes(post, get)
    }
}

data class Routes(val post: HttpService?, val get: HttpService?)

@OptIn(ExperimentalStdlibApi::class)
private class RestServiceHandler(
    private val service: RestService, private val objectMapper: ObjectMapper, private val function: KFunction<*>
) : HttpService {
    private val parameterType = function.parameters[1].type.javaType as Class<*>

    override fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray {
        val parameter = objectMapper.readValue(requestBody, parameterType)
        return runBlocking {
            val result = function.callSuspend(service, parameter)
            objectMapper.writeValueAsBytes(result)
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
private class GetRestServiceHandler(
    private val service: RestService, private val objectMapper: ObjectMapper, private val function: KFunction<*>
) : HttpService {
    private val parameterType = function.parameters[1].type.javaType as Class<*>

    override fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray {
        val parameter = pathVariables["id"]!!.toInt()
        return runBlocking {
            val result = function.callSuspend(service, parameter)
            objectMapper.writeValueAsBytes(result)
        }
    }
}
