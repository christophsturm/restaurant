@file:Suppress("unused")

package restaurant.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import failgood.Test
import failgood.testsAbout
import restaurant.sendRequest
import strikt.api.expectThat
import strikt.assertions.isEqualTo

sealed class LoginResult
class SuccesfulLogin(val token: String) : LoginResult()
class LoginFailed(val message: String) : LoginResult()

@Test
class SealedClassTest {
    val context =
        testsAbout("Different return types with sealed classes") {
            it("is supported") {
                class SealService : RestService {
                    fun show(id: Long): LoginResult {
                        return if (id == 42L) {
                            SuccesfulLogin("1234")
                        } else {
                            LoginFailed("sorry")
                        }
                    }
                }

                val restaurant = autoClose(
                    restaurant {
                        resources(SealService())
                    }
                )
                expectThat(
                    jacksonObjectMapper().readValue<SuccesfulLogin>(
                        restaurant.sendRequest("/seals/42").body()!!
                    ).token
                ).isEqualTo("1234")
                expectThat(
                    jacksonObjectMapper().readValue<LoginFailed>(
                        restaurant.sendRequest("/seals/41").body()!!
                    ).message
                ).isEqualTo("sorry")
            }
        }
}
