package restaurant.internal

import restaurant.*

internal fun routes(routesAdder: RoutesAdder, serviceMapping: RoutingDSL.() -> Unit): List<Route> {

    class Routing(private val prefix: String = "") : RoutingDSL {
        val routes = mutableListOf<Route>()


        override fun route(method: Method, path: String, service: SuspendingHandler) {
            routes.add(Route(method, prefix+path, service))
        }


        override fun wrap(wrapper: Wrapper, function: RoutingDSL.() -> Unit) {
            val nested = Routing(prefix)
            nested.function()
            routes += nested.routes.map { it.copy(wrappers = listOf(wrapper) + it.wrappers) }
        }

        override fun resources(service: RestService, path: String, function: ResourceDSL.() -> Unit) {
            routesAdder.routesFor(service, path).forEach { restRoute ->
                route(restRoute.method, restRoute.path, restRoute.handler)
            }
            ResourceDSL(path).function()
        }

        override fun namespace(prefix: String, function: RoutingDSL.() -> Unit) {
            val routing = Routing(this.prefix + prefix + "/")
            routing.function()
            routes += routing.routes
        }

    }
    return Routing("").apply(serviceMapping).routes
}
