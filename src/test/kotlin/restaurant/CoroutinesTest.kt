package restaurant

import failgood.FailGood
import failgood.describe
import kotlinx.coroutines.delay
import okhttp3.Request
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isFalse
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit

fun main() {
    FailGood.runTest()
}

object CoroutinesTest {
    val context = describe("coroutine handling") {
        pending("cancels coroutine scope when the client disconnects") {
            // it seems undertow does not close the exchange when the client disconnects, so we have no way of detecting
            // client disconnects.
            val client = okhttp3.OkHttpClient.Builder().callTimeout(30, TimeUnit.MILLISECONDS).build()
            val delayService = DelayService()
            val restaurant = Restaurant {
                get("/delay", delayService)
            }
            expectThrows<InterruptedIOException> {
                client.newCall(Request.Builder().url("http://localhost:${restaurant.port}/delay").build()).execute()
            }
            delay(200)
            expectThat(delayService).get { afterDelay }.isFalse()
        }
    }
}

class DelayService : HttpService {
    var afterDelay = false
    override suspend fun handle(requestBody: ByteArray?, pathVariables: Map<String, String>): ByteArray {
        delay(100)
        afterDelay = true
        return "OK".toByteArray()
    }
}

