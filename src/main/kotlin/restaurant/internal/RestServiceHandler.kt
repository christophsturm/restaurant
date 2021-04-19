package restaurant.internal

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import restaurant.HttpService
import restaurant.RestService
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.functions
import kotlin.reflect.javaType

@OptIn(ExperimentalStdlibApi::class)
class RestServiceHandler(private val service: RestService, private val objectMapper: ObjectMapper) : HttpService {
    private val functions = service::class.functions.associateBy { it.name }
    private val function: KFunction<*>? = functions["create"]
    private val parameterType = function!!.parameters[1].type.javaType as Class<*>

    override fun handle(requestBody: ByteArray, pathVariables: Map<String, String>): ByteArray {
        val parameter = objectMapper.readValue(requestBody, parameterType)
        return runBlocking {
            val result = function!!.callSuspend(service, parameter)
            objectMapper.writeValueAsBytes(result)
        }
    }
}
