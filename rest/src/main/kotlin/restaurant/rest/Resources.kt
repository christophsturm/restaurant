package restaurant.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import restaurant.JacksonMapper
import restaurant.RestDSL
import restaurant.RoutingDSL
import restaurant.rest.internal.RoutesAdder
import java.util.Locale

interface RestService

internal fun path(service: Any) =
    service::class.simpleName!!.lowercase(Locale.getDefault()).removeSuffix("service")

private val routesAdder = RoutesAdder(JacksonMapper(jacksonObjectMapper()))

@Suppress("UNUSED_PARAMETER")
@RestDSL
class ResourceDSL(resolvedPath: String) {
    fun resources(service: RestService, function: ResourceDSL.() -> Unit = {}) {
        throw NotImplementedError("this function exists only to make a pending test compile")
    }
}

fun RoutingDSL.resources(service: RestService, path: String = path(service), function: ResourceDSL.() -> Unit = {}) {
    routesAdder.routesFor(service, path).forEach { restRoute ->
        route(restRoute.method, restRoute.path, restRoute.handler)
    }
    ResourceDSL(path).function()
}
