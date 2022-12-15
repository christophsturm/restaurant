package restaurant.rest

import failgood.Test
import failgood.describe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import restaurant.JacksonMapper
import restaurant.Restaurant
import restaurant.client.RestaurantResponse
import restaurant.internal.User
import restaurant.sendRequest
import restaurant.sendStreamingRequest
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo

@Test
class StreamingExample {
    @Suppress("unused")
    val tests = describe("streaming via kotlin flows") {
        class StreamService : RestService {
            @Suppress("unused")
            fun index(): Flow<User> {
                return flow {
                    emit(User("5", "userName"))
                    delay(10)
                    emit(User("6", "otherUserName"))
                }
            }
        }

        val restaurant = autoClose(
            Restaurant(mapper = JacksonMapper()) {
                resources(StreamService())
            }
        )

        it("works with the buffering http client") {
            val response = restaurant.sendRequest("/streams")
            expectThat(response) {
                get { statusCode }.isEqualTo(200)
                get { body }.isEqualTo(
                    """{"id":"5","name":"userName"}
                            |{"id":"6","name":"otherUserName"}
                            |
                    """.trimMargin()
                )
            }
        }
        it("works with the streaming http client") {
            val response: RestaurantResponse<Flow<String>> = restaurant.sendStreamingRequest("/streams")
            expectThat(response) {
                get { statusCode }.isEqualTo(200)
            }
            val list = response.body!!.toList()
            expectThat(list).containsExactly(
                """{"id":"5","name":"userName"}""",
                """{"id":"6","name":"otherUserName"}"""
            )
        }
    }
}
