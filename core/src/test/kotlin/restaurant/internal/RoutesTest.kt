package restaurant.internal

import failgood.Test
import failgood.describe
import failgood.mock.mock
import restaurant.Method
import restaurant.Route
import restaurant.SuspendingHandler
import restaurant.Wrapper
import strikt.api.expectThat
import strikt.assertions.containsExactly

@Test
class RoutesTest {
    val context = describe("Routes") {
        test("creates routes for wrapped handlers") {
            val inner = Wrapper { null }
            val outer = Wrapper { null }
            val handler = mock<SuspendingHandler>()
            val routes = routes {
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
