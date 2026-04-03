package restaurant

import kotlinx.coroutines.flow.Flow
import restaurant.client.RequestDSL
import restaurant.client.RestaurantHttpClient
import restaurant.client.RestaurantResponse
import restaurant.client.loadHttpClientFactory

private val defaultHttpClient by lazy { loadHttpClientFactory().create() }

/** make a http request to a restaurant instance. */
suspend fun Restaurant.sendRequest(
    path: String,
    client: RestaurantHttpClient = defaultHttpClient,
    config: RequestDSL.() -> Unit = {}
): RestaurantResponse<String> = client.send("$baseUrl$path", config)

/** make a http request to a restaurant instance and stream the response */
suspend fun Restaurant.sendStreamingRequest(
    path: String,
    client: RestaurantHttpClient = defaultHttpClient,
    config: RequestDSL.() -> Unit = {}
): RestaurantResponse<Flow<String>> =
    client.send("$baseUrl$path", RestaurantHttpClient.BodyHandlerType.AsFlow, config)
