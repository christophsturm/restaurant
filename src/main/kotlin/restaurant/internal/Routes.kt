package restaurant.internal

import restaurant.*

internal fun routes(routesAdder: RoutesAdder, serviceMapping: RoutingDSL.() -> Unit): List<Route> {

    class Routing(private val prefix: String = "") : RoutingDSL {
        val routes = mutableListOf<Route>()

        override fun post(path: String, service: SuspendingHandler) {
            routes.add(Route(Method.POST, path, service))
        }

        override fun get(path: String, service: SuspendingHandler) {
            routes.add(Route(Method.GET, path, service))
        }


        override fun wrap(wrapper: Wrapper, function: RoutingDSL.() -> Unit) {
            val nested = Routing(prefix)
            nested.function()
            routes += nested.routes.map { it.copy(wrappers = listOf(wrapper) + it.wrappers) }
        }

        override fun resources(service: RestService, path: String, function: ResourceDSL.() -> Unit) {
            val resolvedPath = this.prefix + path
            routes.addAll(routesAdder.routesFor(service, resolvedPath).map { restRoute ->
                val needsBody = restRoute.method != Method.GET
                val restHandler =
                    RestHandler(restRoute.handler, needsBody, if (restRoute.method == Method.POST) 201 else 200)

                Route(restRoute.method, restRoute.path, restHandler, listOf())
            }
            )
            ResourceDSL(resolvedPath).function()
        }

        override fun namespace(prefix: String, function: RoutingDSL.() -> Unit) {
            val routing = Routing(this.prefix + prefix + "/")
            routing.function()
            routes += routing.routes
        }

    }
    return Routing("").apply(serviceMapping).routes
}
