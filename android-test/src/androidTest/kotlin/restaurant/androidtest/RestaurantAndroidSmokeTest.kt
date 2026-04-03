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
import restaurant.client.Java11HttpClient
import restaurant.response

@RunWith(AndroidJUnit4::class)
class RestaurantAndroidSmokeTest {
    @Test
    fun currentServerAndClientApisRoundTripOnAndroid() = runBlocking {
        val restaurant =
            Restaurant {
                route(Method.GET, "ping") { _, _ -> response("pong") }
            }

        try {
            val response = Java11HttpClient(HttpClientConfig(restaurant.baseUrl)).send("/ping")
            assertTrue(response.isOk)
            assertEquals("pong", response.body)
        } finally {
            restaurant.close()
        }
    }
}
