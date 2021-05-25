package restaurant.internal

import restaurant.*

internal fun routes(routesAdder: RoutesAdder, serviceMapping: RoutingDSL.() -> Unit): List<Route> {

    class Routing(private val prefix: String = "") : RoutingDSL {
        val routes = mutableListOf<Route>()

        override fun post(path: String, service: HttpService) {
            routes.add(Route(Method.POST, path, service))
        }

        override fun get(path: String, service: HttpService) {
            routes.add(Route(Method.GET, path, service))
        }


        override fun wrap(wrapper: Wrapper, function: RoutingDSL.() -> Unit) {
            val nested = Routing(prefix)
            nested.function()
            routes += nested.routes.map { it.copy(wrappers = listOf(wrapper) + it.wrappers) }
        }

        override fun resources(service: RestService, path: String, function: ResourceDSL.() -> Unit) {
            val resolvedPath = this.prefix + path
            routes.addAll(routesAdder.routesFor(service, resolvedPath))
            ResourceDSL(resolvedPath).function()
        }

        override fun namespace(prefix: String, function: RoutingDSL.() -> Unit) {
            val routing = Routing(this.prefix + prefix + "/")
            routing.function()
            routes += routing.routes
        }

        override fun resource(service: RestService) {

        }
    }
    return Routing("").apply(serviceMapping).routes
}
