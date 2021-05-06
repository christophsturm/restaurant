package restaurant.internal

import restaurant.RestService
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.reflect.javaType

@OptIn(ExperimentalStdlibApi::class)
class RestFunction(private val function: KFunction<*>, private val service: RestService) {
    private val parameters = function.parameters.drop(1)

    private fun KParameter.isId(): Boolean {
        val javaType = type.javaType
        return javaType == String::class.java || javaType == Int::class.java || javaType == Long::class.java
    }

    val parameterType: Class<*>? = parameters.singleOrNull { !it.isId() }?.type?.javaType as? Class<*>

    suspend fun callSuspend(parameter: Any? = null, pathVariables: Map<String, String>): Any? =
        if (parameter == null)
            function.callSuspend(service)
        else
            function.callSuspend(service, parameter)
}
