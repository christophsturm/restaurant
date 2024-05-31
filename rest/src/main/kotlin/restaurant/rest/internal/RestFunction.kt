package restaurant.rest.internal

import restaurant.RequestContext
import restaurant.RestaurantException
import restaurant.rest.RestService
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.*
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.javaType

class RestFunction(private val function: KFunction<*>, private val service: RestService) {
    private val parameters = function.parameters.drop(1)

    private fun KParameter.isId(): Boolean {
        val javaType = type.javaType
        return javaType == String::class.java || javaType == Int::class.java || javaType == Long::class.java
    }

    private fun KParameter.isPayload(): Boolean {
        val javaType = type.javaType
        return !(
            javaType == String::class.java ||
                javaType == Int::class.java ||
                javaType == Long::class.java ||
                javaType == RequestContext::class.java
            )
    }

    val payloadType: Class<*>? = parameters.filter { it.isPayload() }
        .also { if (it.size > 1) throw MultiplePossibleBodyTypesException(function, it) }
        .singleOrNull()?.type?.javaType as? Class<*>
    private val idParameter = parameters.singleOrNull { it.isId() }?.type?.classifier
    private val instanceParameter = function.instanceParameter!!

    suspend fun callSuspend(payload: Any? = null, id: String?, requestContext: RequestContext): Any? {
        val parameterMap = parameters.associateWithTo(mutableMapOf(instanceParameter to service)) { parameter ->
            val value = when (val javaType = parameter.type.javaType) {
                Long::class.java, Int::class.java, String::class.java -> {
                    id(id!!)
                }

                payloadType -> payload!!
                RequestContext::class.java -> requestContext

                else -> throw RuntimeException("unexpected type $javaType. ")
            }
            value
        }
        return try {
            function.callSuspendBy(parameterMap)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    private fun id(id: String): Any {
        return when (idParameter) {
            Int::class -> id.toInt()
            Long::class -> id.toLong()
            else -> id
        }
    }
}

class MultiplePossibleBodyTypesException(function: KFunction<*>, it: List<KParameter>) : RestaurantException(
    "Rest method ${function.niceName()} has multiple possible body types: ${it.map { it.type.shortName() }}"
)

private fun <R> KCallable<R>.niceName(): String = "${this.parameters.first().type.shortName()}#${this.name}(${
    parameters.drop(1).joinToString { it.type.shortName() ?: "" }
})"

private fun KType.shortName(): String? = (this.classifier as KClass<*>).simpleName
