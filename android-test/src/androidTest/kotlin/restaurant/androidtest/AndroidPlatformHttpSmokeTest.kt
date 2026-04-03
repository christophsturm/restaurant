package restaurant.androidtest

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class AndroidPlatformHttpSmokeTest {
    @Test
    fun urlConnectionConstructsOnAndroid() {
        val connection = URL("http://127.0.0.1").openConnection()
        assertNotNull(connection)
        assertEquals("http", connection.url.protocol)
    }
}
