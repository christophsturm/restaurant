package restaurant.client

import failgood.Test
import failgood.testCollection
import kotlinx.coroutines.flow.toList
import restaurant.*
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

@Test
class HttpClientTest {
    val context =
        testCollection(RestaurantHttpClient::class) {
            forEachClientAndServer { client, server ->
                fun clientFor(restaurant: Restaurant) =
                    autoClose(client.clientFactory.create(HttpClientConfig(restaurant.baseUrl)))

                val restaurant =
                    autoClose(
                        Restaurant(serverFactory = server.serverFactory) {
                            route(Method.GET, "get") { _, _ -> response("get reply") }
                            route(Method.GET, "empty_get") { _, _ -> response() }
                            route(Method.POST, "post") { _, _ ->
                                response(
                                    HttpStatus.TEAPOT_418,
                                    "post\nreply",
                                    mapOf("Content-Type" to "only the best content"))
                            }
                        })
                val httpClient = clientFor(restaurant)

                describe("standalone") {
                    it("can send url requests") {
                        expectThat(httpClient.send("/get").body).isEqualTo("get reply")
                    }
                }

                describe("get requests") {
                    it("are default") {
                        expectThat(restaurant.sendRequest("/get", httpClient).body)
                            .isEqualTo("get reply")
                    }
                    it("can have empty replies") {
                        expectThat(restaurant.sendRequest("/empty_get", httpClient).body)
                            .isEqualTo("")
                    }
                }
                describe("http response") {
                    val response = restaurant.sendRequest("/post", httpClient) { post() }
                    describe("toString method") {
                        it("contains the url") { expectThat(response.toString()).contains("/post") }
                        it("contains the body") {
                            expectThat(response.toString()).contains("body:\"post\nreply\"")
                        }
                        it("contains the status code") {
                            expectThat(response.toString()).contains("""status: 418""")
                        }
                        it("contains the headers") {
                            expectThat(response.toString())
                                .contains("""content-type=[only the best content]""")
                        }
                    }
                }
                describe("streaming the response") {
                    it("works") {
                        val response =
                            httpClient.send("/post", RestaurantHttpClient.BodyHandlerType.AsFlow) {
                                post()
                            }
                        expectThat(response.body?.toList())
                            .isNotNull()
                            .containsExactly("post", "reply")
                    }
                }
                describe("query parameters") {
                    val restaurant =
                        autoClose(
                            Restaurant(serverFactory = server.serverFactory) {
                                route(Method.GET, "with-params") { request, _ ->
                                    val params = request.queryParameters
                                    response(
                                        "received: ${params["name"]?.joinToString(",") ?: "none"}")
                                }
                            })
                    val httpClient = clientFor(restaurant)

                    it("can send query parameters") {
                        val response = httpClient.send("/with-params?name=test&name=test2")
                        expectThat(response.body).isEqualTo("received: test,test2")
                    }
                    it("handles empty query parameters") {
                        val response = httpClient.send("/with-params")
                        expectThat(response.body).isEqualTo("received: none")
                    }
                    it("handles query parameters with special characters") {
                        val response =
                            httpClient.send("/with-params?name=hello%20world&name=test%26test")
                        expectThat(response.body).isEqualTo("received: hello world,test&test")
                    }
                }
            }
        }
}
