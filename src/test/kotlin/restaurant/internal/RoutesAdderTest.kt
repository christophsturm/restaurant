package restaurant.internal

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import failfast.FailFast
import failfast.describe
import kotlinx.coroutines.runBlocking
import strikt.api.expectThat
import strikt.assertions.getValue
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.single
import java.nio.charset.Charset

fun main() {
    FailFast.runTest()
}
object RoutesAdderTest {
    val context = describe(RoutesAdder::class) {
        val routesAdder = RoutesAdder(jacksonObjectMapper())
        listOf(
            Pair(UsersService(), "service with suspend functions and Int primary keys"),
            Pair(NonSuspendUsersService(), "service with blocking functions and Int primary keys")
/*            Pair(NonSuspendStringKeyUsersService(), "service with blocking functions and String primary keys")*/
        ).forEach { (service, description) ->
            describe("for a $description") {
                val ROOT = "root"
                val routes: Map<Method, List<Route>> = routesAdder.routesFor(service, ROOT).groupBy { it.method }
                it("adds a post route") {
                    expectThat(routes).getValue(Method.POST).single()
                        .and {
                            get { path }.isEqualTo(ROOT)
                            get {
                                runBlocking {
                                    handler.handle(
                                        """{"name":"userName"}""".toByteArray(),
                                        mapOf()
                                    )
                                }!!.decodeToString()
                            }
                                .isEqualTo("""{"id":"userId","name":"userName"}""")
                        }
                }
                describe("get routes") {
                    expectThat(routes).getValue(Method.GET).hasSize(2)
                    val getRoutes = routes[Method.GET]!!
                    it("adds a get detail route") {
                        expectThat(
                            String(
                                getRoutes.single { it.path == "$ROOT/{id}" }.handler.handle(null, mapOf("id" to "5"))!!,
                                Charset.defaultCharset()
                            )
                        ).isEqualTo("""{"id":"5","name":"User 5"}""")
                    }
                    it("adds a get list route") {
                        expectThat(
                            String(
                                getRoutes.single { it.path == ROOT }.handler.handle(null, mapOf())!!,
                                Charset.defaultCharset()
                            )
                        ).isEqualTo("""[{"id":"5","name":"userName"},{"id":"6","name":"userName"}]""")
                    }
                }
                it("adds a put route") {
                    expectThat(routes).getValue(Method.PUT).single()
                        .and {
                            get { path }.isEqualTo("$ROOT/{id}")
                            get {
                                runBlocking {
                                    handler.handle(
                                        """{"name":"userName"}""".toByteArray(),
                                        mapOf("id" to "5")
                                    )
                                }!!.decodeToString()
                            }
                                .isEqualTo("""{"id":"5","name":"userName"}""")
                        }
                }
                it("adds a delete route") {
                    expectThat(routes).getValue(Method.DELETE).single()
                        .and {
                            get { path }.isEqualTo("$ROOT/{id}")
                            get {
                                runBlocking {
                                    handler.handle(
                                        null,
                                        mapOf("id" to "5")
                                    )
                                }!!.decodeToString()
                            }
                                .isEqualTo("""{"status":"user 5 deleted"}""")
                        }
                }
            }
        }
    }
}

