package restaurant.internal

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import failgood.describe
import kotlinx.coroutines.runBlocking
import org.junit.platform.commons.annotation.Testable
import restaurant.JacksonMapper
import restaurant.Method
import restaurant.MutableRequestContext
import restaurant.Route
import restaurant.test.MockRequest
import strikt.api.expectThat
import strikt.assertions.getValue
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.single
import java.util.LinkedList

@Testable
class RoutesAdderTest {
    val context = describe(RoutesAdder::class) {
        val requestContext = MutableRequestContext()
        val routesAdder = RoutesAdder(JacksonMapper(jacksonObjectMapper()))
        listOf(
            Pair(UsersService(), "service with suspend functions and Int primary keys"),
            Pair(NonSuspendUsersService(), "service with blocking functions and Int primary keys"),
            Pair(UsersStringPKService(), "service with suspend functions and String primary keys"),
            Pair(
                NonSuspendStringPKUsersService(),
                "service with blocking functions and String primary keys"
            )
        ).forEach { (service, description) ->
            describe("for a $description") {
                val rootPath = "root"
                val routes: Map<Method, List<Route>> =
                    routesAdder.routesFor(service, rootPath).groupBy { it.method }
                it("adds a post route") {
                    expectThat(routes).getValue(Method.POST).single()
                        .and {
                            get { path }.isEqualTo(rootPath)
                            get {
                                runBlocking {
                                    handler.handle(MockRequest("""{"name":"userName"}"""), requestContext)
                                }.bodyString()
                            }.isEqualTo("""{"id":"userId","name":"userName"}""")
                        }
                }
                describe("get routes") {
                    expectThat(routes).getValue(Method.GET).hasSize(2)
                    val getRoutes = routes[Method.GET]!!
                    it("adds a get detail route") {
                        expectThat(
                            getRoutes.single { it.path == "$rootPath/{id}" }.handler.handle(
                                MockRequest(queryParameters = mapOf("id" to LinkedList(listOf("5")))),
                                requestContext
                            ).bodyString()
                        ).isEqualTo("""{"id":"5","name":"User 5"}""")
                    }
                    it("adds a get list route") {
                        expectThat(
                            getRoutes.single { it.path == rootPath }.handler.handle(
                                MockRequest(),
                                requestContext
                            ).bodyString(),
                        ).isEqualTo("""[{"id":"5","name":"userName"},{"id":"6","name":"userName"}]""")
                    }
                }
                it("adds a put route") {
                    expectThat(routes).getValue(Method.PUT).single()
                        .and {
                            get { path }.isEqualTo("$rootPath/{id}")
                            get {
                                runBlocking {
                                    handler.handle(
                                        MockRequest("""{"name":"userName"}""", mapOf("id" to LinkedList(listOf("5")))),
                                        requestContext
                                    ).bodyString()
                                }
                            }
                                .isEqualTo("""{"id":"5","name":"userName"}""")
                        }
                }
                it("adds a delete route") {
                    expectThat(routes).getValue(Method.DELETE).single()
                        .and {
                            get { path }.isEqualTo("$rootPath/{id}")
                            get {
                                runBlocking {
                                    handler.handle(
                                        MockRequest(queryParameters = mapOf("id" to LinkedList(listOf("5")))),
                                        requestContext
                                    ).bodyString()
                                }
                            }.isEqualTo("""{"status":"user 5 deleted"}""")
                        }
                }
            }
        }
    }
}
