package restaurant

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import failgood.describe
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.isEqualTo

sealed class LoginResult
class SuccesfulLogin(val token: String) : LoginResult()
class LoginFailed(val message: String) : LoginResult()

@Testable
class SealedClassTest {
    val context = describe("Different return types with sealed classes") {
        it("works") {
            class SealedClassService : RestService {
                fun show(id: Long): LoginResult {
                    return if (id == 42L)
                        SuccesfulLogin("1234")
                    else
                        LoginFailed("sorry")
                }
            }

            val restaurant = autoClose(
                restaurant {
                    resources(SealedClassService())
                }
            )
            println(restaurant.routes)
            expectThat(
                jacksonObjectMapper().readValue<SuccesfulLogin>(
                    restaurant.request("/sealedclass/42").body()!!
                ).token
            ).isEqualTo("1234")
            expectThat(
                jacksonObjectMapper().readValue<LoginFailed>(
                    restaurant.request("/sealedclass/41").body()!!
                ).message
            ).isEqualTo("sorry")
        }
    }
}