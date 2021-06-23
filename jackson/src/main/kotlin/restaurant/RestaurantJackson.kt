package restaurant

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun restaurant(
    host: String = "127.0.0.1",
    port: Int = findFreePort(),
    exceptionHandler: ExceptionHandler = __defaultExceptionHandler,
    jackson: ObjectMapper = jacksonObjectMapper(),
    defaultHandler: SuspendingHandler = defaultDefaultHandler,
    serviceMapping: RoutingDSL.() -> Unit
) = Restaurant(host, port, exceptionHandler, restaurant.JacksonMapper(jackson), defaultHandler, serviceMapping)
