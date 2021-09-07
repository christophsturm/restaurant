package restaurant.client

import failgood.Test
import failgood.describe
import restaurant.HttpStatus
import restaurant.Method
import restaurant.Restaurant
import restaurant.request
import restaurant.response
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo

@Test
class HttpClientTest {
    val context = describe(Java11HttpClient::class) {
        val restaurant = autoClose(
            Restaurant {
                route(Method.GET, "get") { _, _ ->
                    response("get reply")
                }
                route(Method.POST, "post") { _, _ ->
                    response(HttpStatus.TEAPOT_418, "post reply")
                }
            }
        )

        describe("get requests") {
            it("are default") {
                expectThat(restaurant.request("/get").body).isEqualTo("get reply")
            }
        }
        describe("http response") {
            val response = restaurant.request("/post") { post() }
            describe("toString method") {
                it("contains the url") {
                    expectThat(response.toString()).contains("/post")
                }
                it("contains the post body") {
                    expectThat(response.toString()).contains("""body:"post reply"""")
                }
                it("contains the status code") {
                    expectThat(response.toString()).contains("""status: 418""")
                }
            }
        }
    }
}
