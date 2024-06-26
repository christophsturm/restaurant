package restaurant.internal

import restaurant.*

internal fun routes(mapper: Mapper?, serviceMapping: RoutingDSL.() -> Unit): List<Route> {
    return Routing(mapper, "").apply(serviceMapping).routes
}

class Routing(val mapper: Mapper?, private val prefix: String) : RoutingDSL {
    val routes = mutableListOf<Route>()

    override fun route(method: Method, path: String, service: SuspendingHandler) {
        routes.add(Route(method, prefix + path, service))
    }

    override fun wrap(wrapper: Wrapper, function: RoutingDSL.() -> Unit) {
        val nested = Routing(mapper, prefix)
        nested.function()
        routes += nested.routes.map { it.copy(wrappers = listOf(wrapper) + it.wrappers) }
    }

    override fun namespace(prefix: String, function: RoutingDSL.() -> Unit) {
        val routing = Routing(mapper, this.prefix + prefix + "/")
        routing.function()
        routes += routing.routes
    }
}
