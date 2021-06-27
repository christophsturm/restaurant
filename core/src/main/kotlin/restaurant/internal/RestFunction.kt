package restaurant.internal

import restaurant.RequestContext
import restaurant.RestService
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.javaType

@OptIn(ExperimentalStdlibApi::class)
class RestFunction(private val function: KFunction<*>, private val service: RestService) {
    private val parameters = function.parameters.drop(1)

    private fun KParameter.isId(): Boolean {
        val javaType = type.javaType
        return javaType == String::class.java || javaType == Int::class.java || javaType == Long::class.java
    }

    private fun KParameter.isPayload(): Boolean {
        val javaType = type.javaType
        return !(javaType == String::class.java || javaType == Int::class.java || javaType == Long::class.java || javaType == RequestContext::class.java)
    }

    val payloadType: Class<*>? = parameters.singleOrNull { it.isPayload() }?.type?.javaType as? Class<*>
    private val idParameter = parameters.singleOrNull { it.isId() }?.type?.classifier
    private val instanceParameter = function.instanceParameter!!

    suspend fun callSuspend(payload: Any? = null, id: String?, requestContext: RequestContext): Any? {
        val parameterMap = parameters.map { parameter ->
            val javaType = parameter.type.javaType
            val value = when (javaType) {
                Long::class.java, Int::class.java, String::class.java -> {
                    id(id!!)
                }
                payloadType -> payload!!
                RequestContext::class.java -> requestContext

                else -> throw RuntimeException("unexpected type $javaType payLoad type was: $payloadType")
            }
            Pair(parameter, value)
        }.toMap().plus(instanceParameter to service)
        return function.callSuspendBy(parameterMap)
    }

    private fun id(id: String): Any {
        return when (idParameter) {
            Int::class -> id.toInt()
            Long::class -> id.toLong()
            else -> id
        }
    }


}
