package restaurant.internal

import failgood.Test
import failgood.mock.mock
import failgood.testCollection
import restaurant.*
import strikt.api.expectThat
import strikt.assertions.containsExactly

@Test
class RoutesTest {
    val context =
        testCollection("Routes") {
            test("creates routes for wrapped handlers") {
                val inner = Wrapper { SuspendingHandler { _, _ -> response(200) } }
                val outer = Wrapper { SuspendingHandler { _, _ -> response(200) } }
                val handler = mock<SuspendingHandler>()
                val routes =
                    routes(null) {
                        wrap(outer) { wrap(inner) { route(Method.GET, "/url", handler) } }
                    }
                expectThat(routes)
                    .containsExactly(Route(Method.GET, "/url", handler, listOf(outer, inner)))
            }
        }
}
