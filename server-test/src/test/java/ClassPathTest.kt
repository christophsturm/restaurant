package restaurant

import failgood.Test
import failgood.testCollection
import restaurant.client.HttpClientConfig
import restaurant.client.Java11HttpClient

@Test
class ClassPathTest {
    val t = testCollection {
        val mock = Restaurant { route(Method.GET, "/") { _, _ -> response() } }

        test("client and server work without any other dependencies") {
            assert(Java11HttpClient(HttpClientConfig(mock.baseUrl)).send("/").isOk)
        }
    }
}
