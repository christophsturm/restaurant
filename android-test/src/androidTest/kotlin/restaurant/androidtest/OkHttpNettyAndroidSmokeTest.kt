package restaurant.androidtest

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import restaurant.Method
import restaurant.Restaurant
import restaurant.client.HttpClientConfig
import restaurant.client.OkHttpClient
import restaurant.netty.NettyRestaurantServerFactory
import restaurant.response
import restaurant.sendRequest

@RunWith(AndroidJUnit4::class)
class OkHttpNettyAndroidSmokeTest {
    @Test
    fun explicitOkHttpClientAndNettyServerRoundTripOnAndroid() = runBlocking {
        val restaurant =
            Restaurant(serverFactory = NettyRestaurantServerFactory()) {
                route(Method.GET, "ping") { _, _ -> response("pong") }
            }
        val httpClient = OkHttpClient(HttpClientConfig(restaurant.baseUrl))

        try {
            val response = restaurant.sendRequest("/ping", httpClient)
            assertTrue(response.isOk)
            assertEquals("pong", response.body)
        } finally {
            httpClient.close()
            restaurant.close()
        }
    }
}
