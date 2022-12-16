package restaurant.internal

import failgood.Test
import failgood.describe
import failgood.mock.mock
import restaurant.Method
import restaurant.RealWrapper
import restaurant.Route
import restaurant.SuspendingHandler
import restaurant.response
import strikt.api.expectThat
import strikt.assertions.containsExactly

@Test
class RoutesTest {
    val context = describe("Routes") {
        test("creates routes for wrapped handlers") {
            val inner = RealWrapper { SuspendingHandler { request, requestContext -> response(200) } }
            val outer = RealWrapper { SuspendingHandler { request, requestContext -> response(200) } }
            val handler = mock<SuspendingHandler>()
            val routes = routes(null) {
                wrap(outer) {
                    wrap(inner) {
                        route(Method.GET, "/url", handler)
                    }
                }
            }
            expectThat(routes).containsExactly(Route(Method.GET, "/url", handler, listOf(outer, inner)))
        }
    }
}
