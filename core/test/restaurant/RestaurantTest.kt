package restaurant

import failgood.Test
import failgood.testCollection
import java.nio.ByteBuffer
import kotlin.test.assertNotNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import restaurant.client.HttpClientConfig
import strikt.api.expectThat
import strikt.assertions.*

@Test
class RestaurantTest {
    val context =
        testCollection(Restaurant::class) {
            forEachClientAndServer { client, server ->
                fun clientFor(restaurant: Restaurant) =
                    autoClose(client.clientFactory.create(HttpClientConfig(restaurant.baseUrl)))

                describe("routing") {
                    val restaurant =
                        autoClose(
                            Restaurant(serverFactory = server.serverFactory) {
                                namespace("/handlers") {
                                    route(Method.POST, "reverser") { ex, _ ->
                                        response(
                                            ByteBuffer.wrap(ex.withBody().body!!.reversedArray()))
                                    }
                                }
                            })
                    val httpClient = clientFor(restaurant)

                    it("exposes routes") {
                        with(assertNotNull(restaurant.routes.single())) {
                            assert(method == Method.POST)
                            assert(path == "/handlers/reverser")
                            assert(this.wrappers.isEmpty())
                        }
                    }
                    it("returns 404 if the route is not found") {
                        val response = restaurant.sendRequest("/unconfigured-url", httpClient)
                        expectThat(response)
                            .get { statusCode() }
                            .isEqualTo(HttpStatus.NOT_FOUND_404)
                    }
                    it("calls handlers with body and returns result") {
                        val response =
                            restaurant.sendRequest("/handlers/reverser", httpClient) {
                                post("""jakob""")
                            }
                        expectThat(response) {
                            get { statusCode() }.isEqualTo(200)
                            get { body() }.isEqualTo("bokaj")
                        }
                    }
                    it("prefers literal routes over path parameters") {
                        val restaurant =
                            autoClose(
                                Restaurant(serverFactory = server.serverFactory) {
                                    route(Method.GET, "/users/{id}") { request, _ ->
                                        response(
                                            "param:${request.queryParameters["id"]!!.single()}")
                                    }
                                    route(Method.GET, "/users/me") { _, _ -> response("literal") }
                                })
                        val httpClient = clientFor(restaurant)
                        expectThat(restaurant.sendRequest("/users/me", httpClient)) {
                            get { statusCode() }.isEqualTo(200)
                            get { body() }.isEqualTo("literal")
                        }
                    }
                    it("exposes path parameters to handlers") {
                        val restaurant =
                            autoClose(
                                Restaurant(serverFactory = server.serverFactory) {
                                    route(Method.GET, "/{prefix}/blah") { request, _ ->
                                        response(request.queryParameters["prefix"]!!.single())
                                    }
                                })
                        val httpClient = clientFor(restaurant)
                        expectThat(restaurant.sendRequest("/1234/blah", httpClient)) {
                            get { statusCode() }.isEqualTo(200)
                            get { body() }.isEqualTo("1234")
                        }
                    }
                    it("exposes multiple path parameters to handlers") {
                        val restaurant =
                            autoClose(
                                Restaurant(serverFactory = server.serverFactory) {
                                    route(Method.GET, "/teams/{teamId}/users/{userId}") { request, _
                                        ->
                                        response(
                                            "${request.queryParameters["teamId"]!!.single()}:${request.queryParameters["userId"]!!.single()}")
                                    }
                                })
                        val httpClient = clientFor(restaurant)
                        expectThat(restaurant.sendRequest("/teams/blue/users/42", httpClient)) {
                            get { statusCode() }.isEqualTo(200)
                            get { body() }.isEqualTo("blue:42")
                        }
                    }
                    it("keeps query parameters when path parameters use different names") {
                        val restaurant =
                            autoClose(
                                Restaurant(serverFactory = server.serverFactory) {
                                    route(Method.GET, "/teams/{teamId}") { request, _ ->
                                        response(
                                            "${request.queryParameters["teamId"]!!.single()}:${request.queryParameters["view"]!!.single()}")
                                    }
                                })
                        val httpClient = clientFor(restaurant)
                        expectThat(restaurant.sendRequest("/teams/blue?view=full", httpClient)) {
                            get { statusCode() }.isEqualTo(200)
                            get { body() }.isEqualTo("blue:full")
                        }
                    }
                    it("returns the default handler when segment counts differ") {
                        val restaurant =
                            autoClose(
                                Restaurant(serverFactory = server.serverFactory) {
                                    route(Method.GET, "/users/{id}") { _, _ -> response("matched") }
                                })
                        val httpClient = clientFor(restaurant)
                        expectThat(restaurant.sendRequest("/users/123/profile", httpClient)) {
                            get { statusCode() }.isEqualTo(404)
                        }
                    }
                }
                describe("error handling") {
                    class ExceptionsHandler : SuspendingHandler {
                        override suspend fun handle(
                            request: Request,
                            requestContext: MutableRequestContext
                        ): Response {
                            throw RuntimeException("error message")
                        }
                    }

                    it("returns status 500 per default on error") {
                        val restaurant =
                            autoClose(
                                Restaurant(serverFactory = server.serverFactory) {
                                    route(Method.GET, "/", ExceptionsHandler())
                                })
                        val httpClient = clientFor(restaurant)
                        expectThat(restaurant.sendRequest("/", httpClient)) {
                            get { statusCode() }.isEqualTo(500)
                            get { body }.isNotNull().contains("internal server error")
                        }
                    }
                    it("calls error handler to create error reply") {
                        val restaurant =
                            autoClose(
                                Restaurant(
                                    exceptionHandler = { ex: Throwable ->
                                        response(status = 418, result = "sorry: " + ex.message)
                                    },
                                    serverFactory = server.serverFactory) {
                                        route(Method.GET, "/", ExceptionsHandler())
                                    })
                        val httpClient = clientFor(restaurant)
                        expectThat(restaurant.sendRequest("/", httpClient)) {
                            get { statusCode() }.isEqualTo(418)
                            get { body() }.isEqualTo("sorry: error message")
                        }
                    }
                    it("handles errors in the error handler gracefully") {
                        val restaurant =
                            autoClose(
                                Restaurant(
                                    exceptionHandler = {
                                        throw Exception("oops error handler failed")
                                    },
                                    serverFactory = server.serverFactory) {
                                        route(Method.GET, "/", ExceptionsHandler())
                                    })
                        val httpClient = clientFor(restaurant)
                        expectThat(restaurant.sendRequest("/", httpClient)) {
                            get { statusCode() }.isEqualTo(500)
                            get { body }
                                .isNotNull()
                                .contains("error in error handler")
                                .contains("oops error handler failed")
                        }
                    }
                    it("calls default handler if no suitable route is found") {
                        val restaurant =
                            autoClose(
                                Restaurant(
                                    defaultHandler = { _: Request, _: RequestContext ->
                                        response(418, "not found but anyway I'm teapot")
                                    },
                                    serverFactory = server.serverFactory) {})
                        val httpClient = clientFor(restaurant)
                        expectThat(restaurant.sendRequest("/not-found", httpClient)) {
                            get { statusCode() }.isEqualTo(418)
                            get { body() }.isEqualTo("not found but anyway I'm teapot")
                        }
                    }
                }
                it("exposes its base url for easier testing") {
                    val restaurant =
                        autoClose(
                            Restaurant(
                                host = "0.0.0.0",
                                port = 0,
                                serverFactory = server.serverFactory) {})
                    expectThat(restaurant.baseUrl) {
                        startsWith("http://0.0.0.0:")
                        not { endsWith(":0") }
                    }
                }
                it("can be called with null as port for autodetect") {
                    val restaurant =
                        autoClose(Restaurant(port = null, serverFactory = server.serverFactory) {})
                    val httpClient = clientFor(restaurant)
                    assert(
                        restaurant.sendRequest("/", httpClient).statusCode ==
                            HttpStatus.NOT_FOUND_404)
                }
                it("can be called with port = 0 for random port assignment") {
                    val restaurant =
                        autoClose(Restaurant(port = 0, serverFactory = server.serverFactory) {})
                    val httpClient = clientFor(restaurant)
                    expectThat(restaurant.baseUrl) {
                        startsWith("http://127.0.0.1:")
                        not { endsWith(":0") }
                    }
                    val actualPort = restaurant.baseUrl.substringAfterLast(":").toInt()
                    expectThat(actualPort).isGreaterThan(0)
                    assert(
                        restaurant.sendRequest("/", httpClient).statusCode ==
                            HttpStatus.NOT_FOUND_404)
                }
                describe("to string method for request") {
                    val toString = CompletableDeferred<String>()
                    val restaurant =
                        autoClose(
                            Restaurant(serverFactory = server.serverFactory) {
                                route(Method.GET, "/path") { req, _ ->
                                    toString.complete(req.toString())
                                    response()
                                }
                            })
                    val httpClient = clientFor(restaurant)

                    it("works without query string") {
                        restaurant.sendRequest("/path", httpClient)
                        val await = toString.await()
                        assert(await.contains("/path"))
                        assert(!await.contains("/path?"))
                    }
                    it("works with query string") {
                        restaurant.sendRequest("/path?blah", httpClient)
                        assert(toString.await().contains("/path?blah"))
                    }
                }
                describe("async roundtrip") {
                    it("works") {
                        var stopFlow = false
                        val californiaStreaming = flow {
                            while (!stopFlow) {
                                this.emit("california\n")
                                delay(10)
                            }
                        }

                        val restaurant =
                            autoClose(
                                Restaurant(serverFactory = server.serverFactory) {
                                    route(Method.GET, "/async") { _, _ ->
                                        FlowResponse(mapOf(), 200, californiaStreaming)
                                    }
                                })
                        val httpClient = clientFor(restaurant)
                        val response = restaurant.sendStreamingRequest("/async", httpClient)
                        expectThat(response).get { statusCode }.isEqualTo(200)
                        var received = 0
                        expectThat(
                            response.body!!
                                .map {
                                    if (received++ > 10) {
                                        stopFlow = true
                                    }
                                    it
                                }
                                .toList()) {
                                size.isGreaterThan(10)
                                all { isEqualTo("california") }
                            }
                    }
                }
            }
        }
}
