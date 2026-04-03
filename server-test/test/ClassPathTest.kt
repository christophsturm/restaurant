package restaurant

import failgood.Test
import failgood.testCollection
import restaurant.client.HttpClientConfig
import restaurant.client.loadHttpClientFactory

@Test
class ClassPathTest {
    val t = testCollection {
        val mock = autoClose(Restaurant { route(Method.GET, "/") { _, _ -> response() } })

        test("client and server work without any other dependencies") {
            val client = autoClose(loadHttpClientFactory().create(HttpClientConfig(mock.baseUrl)))
            assert(client.send("/").isOk)
        }
    }
}
