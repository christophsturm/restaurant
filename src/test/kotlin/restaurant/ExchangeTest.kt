package restaurant

import failgood.describe
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Testable
class ExchangeTest {
    val context = describe(Exchange::class) {
        lateinit var exch: Exchange

        class TestHandler : SuspendingHandler {
            override suspend fun handle(exchange: Exchange, requestContext: RequestContext): Response {
                exch = exchange
                return response(200)
            }
        }

        val restaurant = autoClose(
            Restaurant {
                get("/path", TestHandler())
            }
        )

        it("exposes the query string") {
            val response = request(restaurant, "/path?query=string")
            expectThat(response).get { code }.isEqualTo(200)
            expectThat(exch.queryString).isEqualTo("query=string")
        }
        it("exposes the request path") {
            val response = request(restaurant, "/path?query=string")
            expectThat(response).get { code }.isEqualTo(200)
            expectThat(exch.requestPath).isEqualTo("/path")
        }

    }
}

