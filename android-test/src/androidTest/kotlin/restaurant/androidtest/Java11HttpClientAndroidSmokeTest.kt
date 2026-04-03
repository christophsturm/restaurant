package restaurant.androidtest

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import restaurant.client.HttpClientConfig
import restaurant.client.Java11HttpClient

@RunWith(AndroidJUnit4::class)
class Java11HttpClientAndroidSmokeTest {
    @Test
    fun java11HttpClientConstructsOnAndroid() {
        val client = Java11HttpClient(HttpClientConfig("http://127.0.0.1"))
        assertNotNull(client)
    }
}
