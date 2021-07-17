package restaurant.client

import failgood.describe
import org.junit.platform.commons.annotation.Testable
import restaurant.Method
import restaurant.Restaurant
import restaurant.request
import restaurant.response
import strikt.api.expectThat
import strikt.assertions.contains

@Testable
class HttpClientTest {
    val context = describe(Java11HttpClient::class) {
        val restaurant = autoClose(
            Restaurant {
                route(Method.POST, "post") { _, _ ->
                    response("post reply")
                }
            }
        )

        describe("http response") {
            val response = restaurant.request("/post") { post() }
            describe("toString method") {
                it("contains the url") {
                    expectThat(response.toString()).contains("/post")
                }
            }
        }
    }
}
