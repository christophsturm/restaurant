package restaurant.internal

import restaurant.RestService
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.javaType

@OptIn(ExperimentalStdlibApi::class)
class RestFunction(private val function: KFunction<*>, private val service: RestService) {
    val parameterType: Class<*>? = function.parameters.getOrNull(1)?.type?.javaType as? Class<*>

    suspend fun callSuspend(parameter: Any? = null): Any? =
        if (parameter == null)
            function.callSuspend(service)
        else
            function.callSuspend(service, parameter)
}
