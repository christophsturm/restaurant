package restaurant.internal

import restaurant.RequestContext
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

    val payloadType: Class<*>? = parameters.singleOrNull { !it.isId() }?.type?.javaType as? Class<*>
    private val idParameter = parameters.singleOrNull { it.isId() }?.type?.classifier
    private val idFirst = parameters.firstOrNull()?.isId() ?: false


    suspend fun callSuspend(payload: Any? = null, id: String?, requestContext: RequestContext): Any? =
        when {
            payload == null && id == null -> function.callSuspend(service)
            payload == null && id != null -> function.callSuspend(service, id(id))
            id == null -> function.callSuspend(service, payload)
            idFirst -> function.callSuspend(service, id(id), payload)
            else -> function.callSuspend(service, payload, id(id))
        }

    private fun id(id: String): Any {
        return when (idParameter) {
            Int::class -> id.toInt()
            Long::class -> id.toLong()
            else -> id
        }
    }


}
