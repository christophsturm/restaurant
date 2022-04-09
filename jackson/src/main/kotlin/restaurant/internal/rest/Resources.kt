package restaurant.internal.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import restaurant.JacksonMapper
import restaurant.ResourceDSL
import restaurant.RestService
import restaurant.RoutingDSL
import restaurant.internal.RoutesAdder
import java.util.Locale

private fun path(service: RestService) =
    service::class.simpleName!!.lowercase(Locale.getDefault()).removeSuffix("service")


private val routesAdder = RoutesAdder(JacksonMapper(jacksonObjectMapper()))

fun RoutingDSL.resources(service: RestService, path: String = path(service), function: ResourceDSL.() -> Unit = {}) {
    routesAdder.routesFor(service, path).forEach { restRoute ->
        route(restRoute.method, restRoute.path, restRoute.handler)
    }
    ResourceDSL(path).function()
}
