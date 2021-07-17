package restaurant

import restaurant.client.Java11HttpClient

val httpClient = Java11HttpClient()

/**
 * make a http request to a restaurant instance.
 */
suspend fun Restaurant.request(
    path: String,
    config: Java11HttpClient.RequestDSL.() -> Unit = { }
) = httpClient.send("http://localhost:$port$path", config)



