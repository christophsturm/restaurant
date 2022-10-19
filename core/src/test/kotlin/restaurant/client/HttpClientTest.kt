package restaurant.client

import failgood.Test
import failgood.describe
import kotlinx.coroutines.flow.toList
import restaurant.HttpStatus
import restaurant.Method
import restaurant.Restaurant
import restaurant.httpClient
import restaurant.response
import restaurant.sendRequest
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

@Test
class HttpClientTest {
    val context = describe(Java11HttpClient::class) {
        val restaurant = autoClose(
            Restaurant {
                route(Method.GET, "get") { _, _ ->
                    response("get reply")
                }
                route(Method.POST, "post") { _, _ ->
                    response(HttpStatus.TEAPOT_418, "post\nreply", mapOf("Content-Type" to "only the best content"))
                }
            }
        )

        describe("get requests") {
            it("are default") {
                expectThat(restaurant.sendRequest("/get").body).isEqualTo("get reply")
            }
        }
        describe("http response") {
            val response = restaurant.sendRequest("/post") { post() }
            describe("toString method") {
                it("contains the url") {
                    expectThat(response.toString()).contains("/post")
                }
                it("contains the post body") {
                    expectThat(response.toString()).contains("body:\"post\nreply\"")
                }
                it("contains the status code") {
                    expectThat(response.toString()).contains("""status: 418""")
                }
                ignore("contains the headers") { // toString should contain nicely printed headers
                    expectThat(response.toString()).contains(
                        """headers: {"Content-Type" => ["only the best content"] """
                    )
                }
            }
        }
        describe("streaming the response") {
            it("works") {
                val response = httpClient.sendStreaming("http://localhost:${restaurant.port}${"/post"}") { post() }
                expectThat(response.body?.toList()).isNotNull().containsExactly("post", "reply")
            }
        }
    }
}
