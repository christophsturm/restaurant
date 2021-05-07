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
    private val idParameter = parameters.singleOrNull { it.isId() }?.type?.classifier


    suspend fun callSuspend(parameter: Any? = null, id: String?): Any? =
        when {
            parameter == null && id == null -> function.callSuspend(service)
            parameter == null && id != null -> function.callSuspend(service, id(id))
            id == null -> function.callSuspend(service, parameter)
            else -> function.callSuspend(service, parameter, id(id))
        }

    private fun id(id: String): Any {
        return when (idParameter) {
            Int::class -> id.toInt()
            Long::class -> id.toLong()
            else -> id
        }
    }


}
