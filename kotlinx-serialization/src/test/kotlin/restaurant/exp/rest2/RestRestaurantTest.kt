@file:Suppress("UNUSED_VARIABLE", "BlockingMethodInNonBlockingContext", "unused")

package restaurant.exp.rest2

import failgood.Ignored
import failgood.Test
import failgood.testCollection
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import restaurant.*
import restaurant.rest.RestService
import restaurant.rest2.resources
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.single

/*
new reflection-less type safe rest support.
 uses kotlinx-serialization for now because that is harder to support than jackson.
*/
fun restaurant(serviceMapping: RoutingDSL.() -> Unit) = Restaurant(serviceMapping = serviceMapping)

@Test
object RestRestaurantTest {
    val context =
        testCollection(Restaurant::class) {
            describe("rest services") {
                describe("rest routes") {
                    val r =
                        autoClose(
                            restaurant {
                                namespace("/api") {
                                    resources(UserService()) {
                                        index(ListSerializer(User.serializer())) { index() }
                                        show(User.serializer()) { show(it.intId()) }
                                        create(User.serializer()) { create(it.body) }
                                        update(User.serializer()) { update(it.intId(), it.body) }
                                        //                                update = {
                                        // update(it.intId(), it.body()) }
                                    }
                                }
                            })

                    describe("show method") {
                        suspend fun check() {
                            val response = r.sendRequest("/api/users/5")
                            assert(response.isOk)
                            assert(response.body == """{"id":"5","name":"User 5"}""")
                        }
                        describe("when the resource has a default type") {
                            it("calls show on get request with id") {
                                val restaurant =
                                    autoClose(
                                        restaurant {
                                            namespace("/api") {
                                                resources(UserService(), User.serializer()) {
                                                    show { show(it.intId()) }
                                                }
                                            }
                                        })
                                check()
                            }
                            it("calls show that returns custom type on get request with id") {
                                val restaurant =
                                    autoClose(
                                        restaurant {
                                            namespace("/api") {
                                                resources(UserService(), User.serializer()) {
                                                    show(User.serializer()) { show(it.intId()) }
                                                }
                                            }
                                        })
                                check()
                            }
                        }
                        it("calls show that returns custom type on get request with id") {
                            val restaurant =
                                autoClose(
                                    restaurant {
                                        namespace("/api") {
                                            resources(UserService()) {
                                                show(User.serializer()) { show(it.intId()) }
                                            }
                                        }
                                    })
                            check()
                        }
                    }
                    describe("post requests") {
                        val response =
                            r.sendRequest("/api/users") { post("""{"name":"userName"}""") }
                        it("returns 201 - Created on successful post request") {
                            assert(response.statusCode == HttpStatus.CREATED_201)
                        }
                        it("calls create method on post request") {
                            assert(response.body == """{"id":"userId","name":"userName"}""")
                        }
                    }
                    it("calls index method on get request without id") {
                        val response = r.sendRequest("/api/users")
                        expectThat(response) {
                            get { statusCode() }.isEqualTo(200)
                            get { body() }
                                .isEqualTo(
                                    """[{"id":"5","name":"userName"},{"id":"6","name":"userName"}]""")
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
                    describe("missing", ignored = Ignored.Because("working on it")) {
                        it("calls delete method on delete request") {
                            val response = r.sendRequest("/api/users/5") { delete() }
                            expectThat(response) {
                                get { statusCode() }.isEqualTo(200)
                                get { body() }.isEqualTo("""{"status":"user 5 deleted"}""")
                            }
                        }
                        it("sets json content type") {
                            val response = r.sendRequest("/api/users")
                            expectThat(response)
                                .get { headers().allValues(HttpHeader.CONTENT_TYPE) }
                                .single()
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
                                val restaurant =
                                    Restaurant(
                                        exceptionHandler = { ex: Throwable ->
                                            response(status = 418, result = "sorry: " + ex.message)
                                        }) {
                                            resources(ExceptionsService()) {}
                                        }
                                expectThat(restaurant.sendRequest("/exceptions")) {
                                    get { statusCode() }.isEqualTo(418)
                                    get { body() }.isEqualTo("sorry: error message")
                                }
                            }
                        }
                    }
                }
                describe("streaming rest routes") {
                    val r =
                        autoClose(
                            restaurant {
                                namespace("/api") {
                                    resources(StreamingUserService()) {
                                        streamIndex(User.serializer()) { index() }
                                    }
                                }
                            })
                    it("calls index method on get request without id") {
                        val response = r.sendRequest("/api/streamingusers")
                        expectThat(response) {
                            get { statusCode() }.isEqualTo(200)
                            get { body() }
                                .isEqualTo(
                                    """{"id":"5","name":"userName"}
                            |{"id":"6","name":"userName"}
                            |"""
                                        .trimMargin())
                        }
                    }
                }
            }
        }

    @Serializable data class User(val id: String? = null, val name: String)

    data class Hobby(val name: String)

    class UserService : RestService {
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

    class UserStringPKService : RestService {
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

class StreamingUserService : RestService {
    suspend fun index(): Flow<RestRestaurantTest.User> {
        delay(1)
        return flowOf(
            RestRestaurantTest.User("5", "userName"), RestRestaurantTest.User("6", "userName"))
    }
}
