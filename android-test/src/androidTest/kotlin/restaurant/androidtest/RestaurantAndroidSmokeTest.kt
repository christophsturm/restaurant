package restaurant.androidtest

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.net.HttpURLConnection
import java.net.URL
import restaurant.Method
import restaurant.Restaurant
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
            val response = getText("${restaurant.baseUrl}/ping")
            assertTrue(response.first in 200..299)
            assertEquals("pong", response.second)
        } finally {
            restaurant.close()
        }
    }

    private fun getText(url: String): Pair<Int, String> {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            val statusCode = connection.responseCode
            val body =
                connection.inputStream.bufferedReader().use { reader ->
                    reader.readText()
                }
            return statusCode to body
        } finally {
            connection.disconnect()
        }
    }
}
