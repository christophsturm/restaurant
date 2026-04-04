package restaurant

import failgood.dsl.ContextDSL
import restaurant.cio.CioRestaurantServerFactory
import restaurant.client.Java11HttpClientFactory
import restaurant.client.OkHttpClientFactory
import restaurant.client.RestaurantHttpClientFactory
import restaurant.netty.NettyRestaurantServerFactory
import restaurant.undertow.UndertowRestaurantServerFactory

data class RestaurantServerBackend(val name: String, val serverFactory: RestaurantServerFactory)

data class RestaurantClientBackend(
    val name: String,
    val clientFactory: RestaurantHttpClientFactory
)

private val restaurantServerBackends =
    listOf(
        RestaurantServerBackend("cio", CioRestaurantServerFactory()),
        RestaurantServerBackend("undertow", UndertowRestaurantServerFactory()),
        RestaurantServerBackend("netty", NettyRestaurantServerFactory()),
    )

private val restaurantClientBackends =
    listOf(
        RestaurantClientBackend("java11-client", Java11HttpClientFactory()),
        RestaurantClientBackend("okhttp-client", OkHttpClientFactory()),
    )

suspend fun ContextDSL<Unit>.forEachClientAndServer(
    function: suspend ContextDSL<Unit>.(RestaurantClientBackend, RestaurantServerBackend) -> Unit
) {
    restaurantClientBackends.forEach { client ->
        restaurantServerBackends.forEach { server ->
            describe("with client ${client.name} and server ${server.name}") {
                function(this, client, server)
            }
        }
    }
}
