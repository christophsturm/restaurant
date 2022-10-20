@file:Suppress("unused")

package restaurant.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import failgood.Test
import failgood.describe
import restaurant.sendRequest
import strikt.api.expectThat
import strikt.assertions.isEqualTo

sealed class LoginResult
class SuccesfulLogin(val token: String) : LoginResult()
class LoginFailed(val message: String) : LoginResult()

@Test
class SealedClassTest {
    val context = describe("Different return types with sealed classes") {
        it("is supported") {
            class SealedClassService : RestService {
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
                    resources(SealedClassService())
                }
            )
            expectThat(
                jacksonObjectMapper().readValue<SuccesfulLogin>(
                    restaurant.sendRequest("/sealedclass/42").body()!!
                ).token
            ).isEqualTo("1234")
            expectThat(
                jacksonObjectMapper().readValue<LoginFailed>(
                    restaurant.sendRequest("/sealedclass/41").body()!!
                ).message
            ).isEqualTo("sorry")
        }
    }
}
