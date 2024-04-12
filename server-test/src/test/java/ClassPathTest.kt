package restaurant

import failgood.Test
import failgood.tests
import restaurant.client.Java11HttpClient

@Test
class ClassPathTest {
    val t = tests {
        val mock = Restaurant {
            route(Method.GET, "/") { _, _ ->
                response()
            }
        }

        test("client and server work without any other dependencies") {
            assert(Java11HttpClient(mock.baseUrl).send("/").isOk)
        }
    }
}
