@file:Suppress("UNUSED_VARIABLE", "BlockingMethodInNonBlockingContext", "unused")

package restaurant.exp.rest2

import failgood.Ignored
import failgood.Test
import failgood.describe
import kotlinx.coroutines.delay
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import restaurant.ContentType
import restaurant.HttpHeader
import restaurant.HttpStatus
import restaurant.Method
import restaurant.Request
import restaurant.RequestContext
import restaurant.Response
import restaurant.Restaurant
import restaurant.RestaurantException
import restaurant.RoutingDSL
import restaurant.SuspendingHandler
import restaurant.response
import restaurant.rest.RestService
import restaurant.sendRequest
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.single
import java.util.Locale
import kotlin.reflect.KClass

/*
 new reflection-less rest support. uses kotlinx-serialization for now because that is harder to support than jackson
 */
fun restaurant(serviceMapping: RoutingDSL.() -> Unit) = Restaurant(serviceMapping = serviceMapping)

@Test
class RestRestaurantTest {
    val context = describe(Restaurant::class) {
        describe("rest services") {
            describe("rest routes") {
                val r = autoClose(
                    restaurant {
                        namespace("/api") {
                            resources(UsersService()).apply {
//                                index = { index() },
                                show(User.serializer()) { show(it.intId()) }
                                create(User.serializer()) { create(it.body()) }
//                                create = { create(it.body()) },
//                                update = { update(it.intId(), it.body()) }
                            }
                        }
                    }
                )

                describe("show method") {
                    suspend fun check() {
                        val response = r.sendRequest("/api/users/5")
                        assert(response.isOk)
                        assert(response.body == """{"id":"5","name":"User 5"}""")
                    }
                    describe("when the resource has a default type") {
                        it("calls show on get request with id") {
                            val restaurant = autoClose(
                                restaurant {
                                    namespace("/api") {
                                        resources(UsersService(), User.serializer()).apply {
                                            show { show(it.intId()) }
                                        }
                                    }
                                }
                            )
                            check()
                        }
                        it("calls show that returns custom type on get request with id") {
                            val restaurant = autoClose(
                                restaurant {
                                    namespace("/api") {
                                        resources(UsersService(), User.serializer()).apply {
                                            show(User.serializer()) { show(it.intId()) }
                                        }
                                    }
                                }
                            )
                            check()
                        }
                    }
                    it("calls show that returns custom type on get request with id") {
                        val restaurant = autoClose(
                            restaurant {
                                namespace("/api") {
                                    resources(UsersService()).apply {
                                        show(User.serializer()) { show(it.intId()) }
                                    }
                                }
                            }
                        )
                        check()
                    }
                }
                describe("post requests") {
                    val response = r.sendRequest("/api/users") { post("""{"name":"userName"}""") }
                    it("returns 201 - Created on successful post request") {
                        assert(response.statusCode == HttpStatus.CREATED_201)
                    }
                    it("calls create method on post request") {
                        assert(response.body == """{"id":"userId","name":"userName"}""")
                    }
                }
                describe("missing", ignored = Ignored.Because("working on it")) {
                    it("calls index method on get request without id") {
                        val response = r.sendRequest("/api/users")
                        expectThat(response) {
                            get { statusCode() }.isEqualTo(200)
                            get { body() }.isEqualTo("""[{"id":"5","name":"userName"},{"id":"6","name":"userName"}]""")
                        }
                    }
                    it("calls update method on put request") {
                        val response =
                            r.sendRequest("/api/users/5") { put("""{"name":"userName"}""") }
                        expectThat(response) {
                            get { statusCode() }.isEqualTo(200)
                            get { body() }.isEqualTo("""{"id":"5","name":"userName"}""")
                        }
                    }
                    it("calls delete method on delete request") {
                        val response =
                            r.sendRequest("/api/users/5") { delete() }
                        expectThat(response) {
                            get { statusCode() }.isEqualTo(200)
                            get { body() }.isEqualTo("""{"status":"user 5 deleted"}""")
                        }
                    }
                    it("sets json content type") {
                        val response = r.sendRequest("/api/users")
                        expectThat(response).get { headers().allValues(HttpHeader.CONTENT_TYPE) }.single()
                            .isEqualTo(ContentType.APPLICATION_JSON)
                    }
                    describe("error handling") {
                        describe("malformed requests") {
                            it("returns a useful error message") {
                                val requestBody = """{"nam":"userName"}"""
                                val response = r.sendRequest("/api/users") { post(requestBody) }
                                expectThat(response) {
                                    get { statusCode() }.isEqualTo(HttpStatus.BAD_REQUEST_400)
                                    get { body }.isNotNull().contains(requestBody)
                                }
                            }
                        }
                    }
                    describe("error handling") {
                        class ExceptionsService : RestService {
                            fun index() {
                                throw RuntimeException("error message")
                            }
                        }
                        it("calls error handler with the correct exception") {
                            @Suppress("NAME_SHADOWING")
                            val restaurant = Restaurant(exceptionHandler = { ex: Throwable ->
                                response(
                                    status = 418,
                                    result = "sorry: " + ex.message
                                )
                            }) { resources(ExceptionsService()) }
                            expectThat(restaurant.sendRequest("/exceptions")) {
                                get { statusCode() }.isEqualTo(418)
                                get { body() }.isEqualTo("sorry: error message")
                            }
                        }
                    }
                }
            }
        }
    }

    @Serializable
    data class User(val id: String? = null, val name: String)
    data class Hobby(val name: String)
    class UsersService : RestService {
        suspend fun index(): List<User> {
            delay(1)
            return listOf(User("5", "userName"), User("6", "userName"))
        }

        suspend fun create(user: User): User {
            delay(1)
            return user.copy(id = "userId")
        }

        suspend fun show(userId: Int): User {
            delay(1)
            return User(id = userId.toString(), name = "User $userId")
        }

        suspend fun update(userId: Int, user: User): User {
            delay(1)
            return user.copy(id = userId.toString())
        }

        suspend fun delete(userId: Int): DeleteReply {
            delay(1)
            return DeleteReply("user $userId deleted")
        }
    }

    class UsersStringPKService : RestService {
        suspend fun index(): List<User> {
            delay(1)
            return listOf(User("5", "userName"), User("6", "userName"))
        }

        suspend fun create(user: User): User {
            delay(1)
            return user.copy(id = "userId")
        }

        suspend fun show(userId: String): User {
            delay(1)
            return User(id = userId, name = "User $userId")
        }

        suspend fun update(userId: String, user: User): User {
            delay(1)
            return user.copy(id = userId)
        }

        suspend fun delete(userId: String): DeleteReply {
            delay(1)
            return DeleteReply("user $userId deleted")
        }
    }

    data class DeleteReply(val status: String)
}

private fun <Service : Any> RoutingDSL.resources(service: Service, path: String = path(service)) =
    ResourceMapperImpl(this, service, path)

private fun <Service : Any, ServiceResponse> RoutingDSL.resources(
    service: Service,
    responseSerializer: KSerializer<ServiceResponse>,
    path: String = path(service)
) =
    ResourceMapperWithDefaultType(responseSerializer, ResourceMapperImpl(this, service, path))

class ResourceMapperWithDefaultType<Service : Any, DefaultType>(
    val responseSerializer: KSerializer<DefaultType>,
    val resourceMapper: ResourceMapper<Service>
) : ResourceMapper<Service> by resourceMapper {
    fun show(body: suspend Service.(ShowContext) -> DefaultType) {
        resourceMapper.show(responseSerializer, body)
    }
}

@Suppress("UNUSED_PARAMETER")
interface Context {
    fun <T : Any> get(kClass: KClass<T>): T {
        TODO()
    }

    fun id(): String
    fun intId(): Int
}

private inline fun <reified T : Any> Context.body(): T {
    return this.get(T::class)
}

private fun path(service: Any) =
    service::class.simpleName!!.lowercase(Locale.getDefault()).removeSuffix("service")

interface ResourceMapper<Service : Any> {
    fun <ServiceResponse> show(
        responseSerializer: KSerializer<ServiceResponse>,
        body: suspend Service.(ShowContext) -> ServiceResponse
    )

    fun <RequestAndResponse> create(
        serializer: KSerializer<RequestAndResponse>,
        body: suspend Service.(CreateContext<RequestAndResponse>) -> RequestAndResponse
    )
}

@Suppress("UNUSED_PARAMETER")
class ResourceMapperImpl<Service : Any>(
    private val routingDSL: RoutingDSL,
    private val service: Service,
    private val path: String = path(service)
) : ResourceMapper<Service> {
    override fun <ServiceResponse> show(
        responseSerializer: KSerializer<ServiceResponse>,
        body: suspend Service.(ShowContext) -> ServiceResponse
    ) {
        routingDSL.route(Method.GET, "$path/{id}", ShowHandler(responseSerializer, service, body))
    }

    override fun <RequestAndResponse> create(
        serializer: KSerializer<RequestAndResponse>,
        body: suspend Service.(CreateContext<RequestAndResponse>) -> RequestAndResponse
    ) {
        routingDSL.route(
            Method.POST,
            path,
            CreateHandler(serializer, serializer, service, body)
        )
    }
}

interface CreateContext<ResponseType> {
    fun body(): ResponseType
}

interface ShowContext {
    fun intId(): Int
}

class ShowHandler<Service : Any, ServiceResponse>(
    private val responseSerializer: KSerializer<ServiceResponse>,
    private val service: Service,
    val show: (suspend Service.(ShowContext) -> ServiceResponse)
) : SuspendingHandler {
    override suspend fun handle(request: Request, requestContext: RequestContext): Response {
        val id = request.queryParameters.let {
            it["id"]?.singleOrNull()
                ?: throw RuntimeException("id variable not found. variables: ${it.keys.joinToString()}")
        }
        val result = service.show(ShowContextImpl(id))
        return response(200, Json.encodeToString(responseSerializer, result))
    }
}

class CreateHandler<Service : Any, ServiceRequest, ServiceResponse>(
    private val requestSerializer: KSerializer<ServiceRequest>,
    private val responseSerializer: KSerializer<ServiceResponse>,
    private val service: Service,
    val show: (suspend Service.(CreateContext<ServiceRequest>) -> ServiceResponse)
) : SuspendingHandler {
    override suspend fun handle(request: Request, requestContext: RequestContext): Response {
        val payload = request.withBody().body.let {
            val string = String(it!!)
            try {
                Json.decodeFromString(requestSerializer, string)
            } catch (e: Exception) {
                throw RestaurantException("error deserializing request body: $string")
            }
        }

        val result = service.show(CreateContextImpl(payload))
        return response(201, Json.encodeToString(responseSerializer, result))
    }
}

class CreateContextImpl<ServiceRequest>(val body: ServiceRequest) : CreateContext<ServiceRequest> {
    override fun body(): ServiceRequest = body
}

class ShowContextImpl(private val id: String) : ShowContext {
    override fun intId(): Int {
        return id.toInt()
    }
}
