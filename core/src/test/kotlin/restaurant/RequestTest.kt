package restaurant

import failgood.describe
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Testable
class RequestTest {
    val context = describe(Request::class) {
        lateinit var req: Request

        class TestHandler : SuspendingHandler {
            override suspend fun handle(request: Request, requestContext: RequestContext): Response {
                req = request
                return response(200)
            }
        }

        val restaurant = autoClose(
            Restaurant {
                route(Method.GET, "/path", TestHandler())
            }
        )

        val response = restaurant.request("/path?query=string")
        expectThat(response).get { statusCode() }.isEqualTo(200)
        it("exposes the query string") {
            expectThat(req.queryString).isEqualTo("query=string")
        }
        it("exposes the request path") {
            expectThat(req.requestPath).isEqualTo("/path")
        }
        it("exposes the request method") {
            expectThat(req.method).isEqualTo(Method.GET)
        }

    }
}

