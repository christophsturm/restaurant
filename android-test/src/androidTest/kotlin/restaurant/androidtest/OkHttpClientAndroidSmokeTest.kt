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
import restaurant.client.loadHttpClientFactory
import restaurant.response

@RunWith(AndroidJUnit4::class)
class OkHttpClientAndroidSmokeTest {
    @Test
    fun serviceLoadedHttpClientRoundTripsOnAndroid() = runBlocking {
        val restaurant =
            Restaurant {
                route(Method.GET, "ping") { _, _ -> response("pong") }
            }
        val clientFactory = loadHttpClientFactory()
        val httpClient = clientFactory.create(HttpClientConfig(restaurant.baseUrl))

        try {
            assertEquals("okhttp-client", clientFactory.name)
            val response = httpClient.send("/ping")
            assertTrue(response.isOk)
            assertEquals("pong", response.body)
        } finally {
            httpClient.close()
            restaurant.close()
        }
    }
}
