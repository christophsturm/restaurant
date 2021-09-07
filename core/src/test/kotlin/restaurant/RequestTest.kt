package restaurant

import failgood.Test
import failgood.describe
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
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
            val response = restaurant.request("/path?p1=v1&p1=v2&p2=v3") {
                addHeader("header1", "value1")
                addHeader("header1", "value2")
                addHeader("header2", "value3")
            }
            expectThat(response).get { statusCode() }.isEqualTo(200)
            it("exposes the query string") {
                expectThat(req.queryString).isEqualTo("p1=v1&p1=v2&p2=v3")
            }
            it("exposes the request path") {
                expectThat(req.requestPath).isEqualTo("/path")
            }
            it("exposes the request method") {
                expectThat(req.method).isEqualTo(Method.GET)
            }
            describe("headers") {
                it("can get a list of header values") {
                    expectThat(req.headers["header1"]).isNotNull().containsExactly("value1", "value2")
                }
                it("getSingleOrNull returns a single value if there is one") {
                    expectThat(req.headers.getSingleOrNull("header2")).isEqualTo("value3")
                }
                it("getSingleOrNull returns null if there are 2 values") {
                    expectThat(req.headers.getSingleOrNull("header1")).isNull()
                }
                it("getSingleOrNull returns null if there are no values") {
                    expectThat(req.headers.getSingleOrNull("not-existing-header")).isNull()
                }
            }
            describe("query parameters") {
                it("can get a list of query parameters") {
                    expectThat(req.queryParameters["p1"]).isNotNull().containsExactlyInAnyOrder("v1", "v2")
                }
                it("getSingleOrNull returns a single value if there is one") {
                    expectThat(req.queryParameters.getSingleOrNull("p2")).isEqualTo("v3")
                }
                it("getSingleOrNull returns null if there are 2 values") {
                    expectThat(req.headers.getSingleOrNull("p1")).isNull()
                }
                it("getSingleOrNull returns null if there are no values") {
                    expectThat(req.headers.getSingleOrNull("not-existing-parameter")).isNull()
                }
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


