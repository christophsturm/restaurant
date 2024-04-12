package restaurant

import kotlinx.coroutines.flow.Flow
import restaurant.client.Java11HttpClient
import restaurant.client.RestaurantResponse

private val httpClient = Java11HttpClient()

/**
 * make a http request to a restaurant instance.
 */
suspend fun Restaurant.sendRequest(
    path: String,
    config: Java11HttpClient.RequestDSL.() -> Unit = { }
): RestaurantResponse<String> = httpClient.send("$baseUrl$path", config)

/**
 * make a http request to a restaurant instance and stream the response
 */
suspend fun Restaurant.sendStreamingRequest(
    path: String,
    config: Java11HttpClient.RequestDSL.() -> Unit = { }
): RestaurantResponse<Flow<String>> = httpClient.sendStreaming("$baseUrl$path", config)
