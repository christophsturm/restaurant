package restaurant.internal

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import failfast.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.charset.Charset

object RoutesAdderTest {
    val context = describe(RoutesAdder::class) {
        val routesAdder = RoutesAdder(jacksonObjectMapper())
        val routes = routesAdder.routesFor(UsersService())
        it("adds a post route") {
            expectThat(
                String(
                    routes.post!!.handle("""{"name":"userName"}""".toByteArray(), mapOf())!!,
                    Charset.defaultCharset()
                )
            ).isEqualTo("""{"id":"userId","name":"userName"}""")
        }
        it("adds a get route") {
            expectThat(
                String(
                    routes.get!!.handle(null, mapOf("id" to "5"))!!,
                    Charset.defaultCharset()
                )
            ).isEqualTo("""{"id":"5","name":"User 5"}""")
        }
        it("adds a put route") {
            expectThat(
                String(
                    routes.put!!.handle("""{"name":"userName"}""".toByteArray(), mapOf("id" to "5"))!!,
                    Charset.defaultCharset()
                )
            ).isEqualTo("""{"id":"5","name":"userName"}""")
        }
    }
}
