package restaurant

import failgood.describe
import kotlinx.coroutines.delay
import okhttp3.Request
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isFalse
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit

@Testable
class CoroutinesTest {
    val context = describe("coroutine handling") {
        pending("cancels coroutine scope when the client disconnects") {
            // it seems undertow does not close the exchange when the client disconnects, so we have no way of detecting
            // client disconnects.
            val client = okhttp3.OkHttpClient.Builder().callTimeout(30, TimeUnit.MILLISECONDS).build()
            val delayService = DelayService()
            val restaurant = Restaurant {
                route(Method.GET, "/delay", delayService)
            }
            expectThrows<InterruptedIOException> {
                client.newCall(Request.Builder().url("http://localhost:${restaurant.port}/delay").build()).execute()
            }
            delay(200)
            expectThat(delayService).get { afterDelay }.isFalse()
        }
    }
}

class DelayService : SuspendingHandler {
    var afterDelay = false
    override suspend fun handle(exchange: Exchange, requestContext: RequestContext): Response {
        delay(100)
        afterDelay = true
        return response()
    }
}

