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
        testCollection(Java11HttpClient::class) {
            val restaurant =
                autoClose(
                    Restaurant {
                        route(Method.GET, "get") { _, _ -> response("get reply") }
                        route(Method.GET, "empty_get") { _, _ -> response() }
                        route(Method.POST, "post") { _, _ ->
                            response(
                                HttpStatus.TEAPOT_418,
                                "post\nreply",
                                mapOf("Content-Type" to "only the best content"))
                        }
                    })
            val httpClient = Java11HttpClient(HttpClientConfig(restaurant.baseUrl))

            describe("standalone") {
                it("can send url requests") {
                    expectThat(httpClient.send("/get").body).isEqualTo("get reply")
                }
                it("can send requests") {
                    expectThat(httpClient.send(httpClient.buildRequest("/get")).body)
                        .isEqualTo("get reply")
                }
            }

            describe("get requests") {
                it("are default") {
                    expectThat(restaurant.sendRequest("/get").body).isEqualTo("get reply")
                }
                it("can have empty replies") {
                    expectThat(restaurant.sendRequest("/empty_get").body).isEqualTo("")
                }
            }
            describe("http response") {
                val response = restaurant.sendRequest("/post") { post() }
                describe("toString method") {
                    it("contains the url") { expectThat(response.toString()).contains("/post") }
                    it("contains the post body") {
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
                    val response = httpClient.sendStreaming("/post") { post() }
                    expectThat(response.body?.toList()).isNotNull().containsExactly("post", "reply")
                }
            }
        }
}
