package restaurant

import failgood.Test
import failgood.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs

@Test
class RequestTest {
    val context = describe(Request::class) {
        describe("get requests") {
            lateinit var req: Request
            val restaurant = autoClose(Restaurant {
                route(Method.GET, "/path") { request, _ ->
                    req = request
                    response(200)
                }
            })
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
        describe("body handling") {
            lateinit var req: RequestWithBody
            val restaurant = autoClose(Restaurant {
                route(Method.POST, "/path") { request, _ ->
                    req = request.withBody()
                    response(200)
                }
            })
            val response = restaurant.request("/path?query=string") { post("body") }
            expectThat(response).get { statusCode() }.isEqualTo(200)
            it("can convert to a request that has a body") {
                expectThat(req).get { String(body!!) }.isEqualTo("body")
            }
            it("returns this when it already has a body") {
                val requestWithBody = req.withBody()
                expectThat(requestWithBody).isSameInstanceAs(req)
            }
        }
    }
}

