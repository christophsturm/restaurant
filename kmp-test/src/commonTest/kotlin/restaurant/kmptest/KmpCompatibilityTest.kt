package restaurant.kmptest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import restaurant.Method
import restaurant.Route
import restaurant.RoutingDSL
import restaurant.SuspendingHandler
import restaurant.Wrapper
import restaurant.response
import restaurant.rest2.resources

class KmpCompatibilityTest {
    @Test
    fun common_test_can_define_rest2_routes() {
        val routing = RecordingRouting()

        with(routing) {
            resources(UserService()) {
                index(ListSerializer(User.serializer())) { index() }
                show(User.serializer()) { show(it.intId()) }
                create(User.serializer()) { create(it.body) }
            }
        }

        assertEquals(
            listOf(
                Method.GET to "users",
                Method.GET to "users/{id}",
                Method.POST to "users",
            ),
            routing.routes.map { it.method to it.path },
        )
    }

    @Test
    fun common_test_can_define_low_level_api_routes() {
        val routing = RecordingRouting()

        with(routing) {
            namespace("admin") {
                wrap(Wrapper { it }) {
                    route(Method.DELETE, "users/{id}") { _, _ -> response("deleted") }
                }
            }
        }

        assertEquals(
            listOf(Method.DELETE to "users/{id}"),
            routing.routes.map { it.method to it.path },
        )
    }
}

@Serializable private data class User(val id: String? = null, val name: String)

private class UserService {
    fun index(): List<User> = listOf(User("1", "Ada"))

    fun show(userId: Int): User = User(userId.toString(), "Ada")

    fun create(user: User): User = user.copy(id = "created")
}

private class RecordingRouting : RoutingDSL {
    val routes = mutableListOf<Route>()

    override fun namespace(prefix: String, function: RoutingDSL.() -> Unit) {
        function()
    }

    override fun wrap(wrapper: Wrapper, function: RoutingDSL.() -> Unit) {
        function()
    }

    override fun route(method: Method, path: String, service: SuspendingHandler) {
        routes += Route(method, path, service)
    }
}
