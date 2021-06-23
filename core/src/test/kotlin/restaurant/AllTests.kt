package restaurant

import failgood.FailGood.runAllTests

fun restaurant(
    host: String = "127.0.0.1",
    port: Int = findFreePort(),
    exceptionHandler: ExceptionHandler = __defaultExceptionHandler,
    defaultHandler: SuspendingHandler = defaultDefaultHandler,
    serviceMapping: RoutingDSL.() -> Unit
) = Restaurant(host, port, exceptionHandler, NullMapper, defaultHandler, serviceMapping)


suspend fun main() {
    runAllTests(true)
}
