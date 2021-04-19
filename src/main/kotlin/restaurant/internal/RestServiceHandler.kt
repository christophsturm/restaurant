package restaurant.internal

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.runBlocking
import restaurant.HttpService
import restaurant.RestService
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.functions
import kotlin.reflect.javaType

class RestServiceHandler(private val service: RestService) : HttpService {
    private val functions = service::class.functions.associateBy { it.name }

    @OptIn(ExperimentalStdlibApi::class)
    override fun handle(requestBody: ByteArray): ByteArray {
        val function: KFunction<*>? = functions["create"]
        val parameterType = function!!.parameters[1].type.javaType as Class<*>
        val parameter = jacksonObjectMapper().readValue(requestBody, parameterType)
        return runBlocking {
            val result = function.callSuspend(service, parameter)
            jacksonObjectMapper().writeValueAsBytes(result)
        }
    }
}
