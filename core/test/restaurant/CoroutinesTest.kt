package restaurant

import failgood.Ignored
import failgood.Test
import failgood.testCollection
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import restaurant.client.HttpClientConfig
import restaurant.client.HttpClientException
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isFalse

@Test
class CoroutinesTest {
    val context =
        testCollection("coroutine handling") {
            forEachClientAndServer { client, server ->
                it(
                    "cancels coroutine scope when the client disconnects",
                    ignored =
                        Ignored.Because(
                            "client disconnect cancellation is not wired reliably across the " +
                                "current server backends")) {
                        val delayService = DelayService()
                        val restaurant =
                            autoClose(
                                Restaurant(serverFactory = server.serverFactory) {
                                    route(Method.GET, "/delay", delayService)
                                })
                        val httpClient =
                            autoClose(
                                client.clientFactory.create(HttpClientConfig(restaurant.baseUrl)))
                        expectThrows<HttpClientException> {
                            restaurant.sendRequest("/delay", httpClient) {
                                timeout(30.milliseconds)
                            }
                        }
                        delay(200)
                        expectThat(delayService).get { afterDelay }.isFalse()
                    }
            }
        }
}

class DelayService : SuspendingHandler {
    var afterDelay = false

    override suspend fun handle(request: Request, requestContext: MutableRequestContext): Response {
        delay(100)
        afterDelay = true
        return response()
    }
}
