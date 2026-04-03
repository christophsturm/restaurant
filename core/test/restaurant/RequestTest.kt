package restaurant

import failgood.Test
import failgood.testCollection
import kotlin.test.assertEquals
import restaurant.client.HttpClientConfig
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

@Test
class RequestTest {
    val context =
        testCollection(Request::class) {
            forEachClientAndServer { client, server ->
                fun clientFor(restaurant: Restaurant) =
                    autoClose(client.clientFactory.create(HttpClientConfig(restaurant.baseUrl)))

                describe("get requests") {
                    lateinit var req: Request
                    val restaurant =
                        autoClose(
                            Restaurant(serverFactory = server.serverFactory) {
                                route(Method.GET, "/path") { request, _ ->
                                    req = request
                                    response(200)
                                }
                            })
                    val httpClient = clientFor(restaurant)
                    val response =
                        restaurant.sendRequest("/path?p1=v1&p1=v2&p2=v3", httpClient) {
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
                            expectThat(req.headers["header1"])
                                .isNotNull()
                                .containsExactly("value1", "value2")
                        }
                    }
                    describe("query parameters") {
                        it("can get a list of query parameters") {
                            expectThat(req.queryParameters["p1"])
                                .isNotNull()
                                .containsExactlyInAnyOrder("v1", "v2")
                        }
                    }
                }
                describe("body handling") {
                    describe("without wrapper") {
                        lateinit var req: RequestWithBody
                        val restaurant =
                            autoClose(
                                Restaurant(serverFactory = server.serverFactory) {
                                    route(Method.POST, "/path") { request, _ ->
                                        req = request.withBody()
                                        response(200)
                                    }
                                })
                        val httpClient = clientFor(restaurant)
                        val response =
                            restaurant.sendRequest("/path?query=string", httpClient) {
                                post("body")
                            }
                        assert(response.isOk)
                        it("can convert to a request that has a body") {
                            assert(String(req.body!!) == "body")
                        }
                        it("returns this when it already has a body") {
                            val requestWithBody = req.withBody()
                            assert(requestWithBody === req)
                        }
                        it("includes body in toString") {
                            assertEquals(
                                "Request(method:POST, path:/path?query=string, body:body)",
                                req.toString())
                        }
                    }
                    describe("with wrapper") {
                        lateinit var req: RequestWithBody
                        val restaurant =
                            autoClose(
                                Restaurant(serverFactory = server.serverFactory) {
                                    wrap(BodyReader()) {
                                        route(Method.POST, "/path") { request, _ ->
                                            req = request.withBody()
                                            response(200)
                                        }
                                    }
                                })
                        val httpClient = clientFor(restaurant)
                        val response =
                            restaurant.sendRequest("/path?query=string", httpClient) {
                                post("body")
                            }
                        assert(response.isOk)
                        it("can convert to a request that has a body") {
                            assert(String(req.body!!) == "body")
                        }
                    }
                }
            }
        }
}

class BodyReader : Wrapper {
    override fun wrap(wrapped: SuspendingHandler): SuspendingHandler =
        SuspendingHandler { request, requestContext ->
            wrapped.handle(request.withBody(), requestContext)
        }
}
