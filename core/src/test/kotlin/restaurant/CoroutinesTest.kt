package restaurant

import failgood.describe
import kotlinx.coroutines.delay
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isFalse
import java.net.http.HttpTimeoutException
import java.time.temporal.ChronoUnit

@Testable
class CoroutinesTest {
    val context = describe("coroutine handling") {
        pending("cancels coroutine scope when the client disconnects") {
            // it seems undertow does not close the exchange when the client disconnects, so we have no way of detecting
            // client disconnects.
            val delayService = DelayService()
            val restaurant = Restaurant {
                route(Method.GET, "/delay", delayService)
            }
            expectThrows<HttpTimeoutException> {
                restaurant.request("/delay") { timeout(30, ChronoUnit.MILLIS) }
            }
            delay(200)
            expectThat(delayService).get { afterDelay }.isFalse()
        }
    }
}

class DelayService : SuspendingHandler {
    var afterDelay = false
    override suspend fun handle(request: Request, requestContext: RequestContext): Response {
        delay(100)
        afterDelay = true
        return response()
    }
}

