package restaurant

import failgood.dsl.ContextDSL
import restaurant.netty.NettyRestaurantServerFactory
import restaurant.undertow.UndertowRestaurantServerFactory

data class RestaurantServerBackend(val name: String, val serverFactory: RestaurantServerFactory)

val restaurantServerBackends =
    listOf(
        RestaurantServerBackend("undertow", UndertowRestaurantServerFactory()),
        RestaurantServerBackend("netty", NettyRestaurantServerFactory()),
    )

suspend fun ContextDSL<Unit>.forEachBackend(
    function: suspend ContextDSL<Unit>.(RestaurantServerBackend) -> Unit
) {
    restaurantServerBackends.forEach { backend ->
        describe(backend.name) { function(this, backend) }
    }
}
