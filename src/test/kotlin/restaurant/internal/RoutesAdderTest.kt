package restaurant.internal

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import failfast.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.charset.Charset

object RoutesAdderTest {
    val context = describe(RoutesAdder::class) {
        val routesAdder = RoutesAdder(jacksonObjectMapper())
        listOf(
            Pair(UsersService(), "service with suspend functions and Int primary keys"),
            Pair(NonSuspendUsersService(), "service with blocking functions and Int primary keys")
/*            Pair(NonSuspendStringKeyUsersService(), "service with blocking functions and String primary keys")*/
        ).forEach { (service, description) ->
            describe("for a $description") {
                val routes = routesAdder.routesFor(service)
                it("adds a post route") {
                    expectThat(
                        String(
                            routes.post!!.handle("""{"name":"userName"}""".toByteArray(), mapOf())!!,
                            Charset.defaultCharset()
                        )
                    ).isEqualTo("""{"id":"userId","name":"userName"}""")
                }
                it("adds a get detail route") {
                    expectThat(
                        String(
                            routes.get!!.handle(null, mapOf("id" to "5"))!!,
                            Charset.defaultCharset()
                        )
                    ).isEqualTo("""{"id":"5","name":"User 5"}""")
                }
                it("adds a get list route") {
                    expectThat(
                        String(
                            routes.getList!!.handle(null, mapOf())!!,
                            Charset.defaultCharset()
                        )
                    ).isEqualTo("""[{"id":"5","name":"userName"},{"id":"6","name":"userName"}]""")
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
    }
}
