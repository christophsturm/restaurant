package restaurant.internal

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import failgood.describe
import failgood.mock.mock
import org.junit.platform.commons.annotation.Testable
import restaurant.ExchangeWrapper
import restaurant.HttpService
import restaurant.Wrapper
import strikt.api.expectThat
import strikt.assertions.containsExactly

@Testable
class RoutesTest {
    val context = describe("Routes") {
        test("creates routes for wrapped handlers") {
            val inner = object : Wrapper {
                override suspend fun invoke(exchange: ExchangeWrapper) = null
            }
            val outer = object : Wrapper {
                override suspend fun invoke(exchange: ExchangeWrapper) = null
            }
            val handler = mock<HttpService>()
            val routes = routes(RoutesAdder(jacksonObjectMapper())) {
                wrap(outer) {
                    wrap(inner) {
                        get("/url", handler)
                    }
                }
            }
            expectThat(routes).containsExactly(Route(Method.GET, "/url", handler, listOf(outer, inner)))
        }
    }

}
