package restaurant

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import failgood.describe
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

object JWTConfig {
    private const val issuer = "https://restaurant.dev/"
    private const val audience = "jwtAudience"
    private val algorithm = Algorithm.HMAC256("psst, secret")!!
    fun makeToken(userId: Long): String = JWT.create()
        .withAudience(audience)
        .withSubject("Authentication")
        .withIssuer(issuer)
        .withClaim("jti", userId)
        .sign(algorithm)!!

    fun makeJwtVerifier(): JWTVerifier = JWT.require(algorithm)
        .withAudience(audience)
        .withIssuer(issuer)
        .build()
}

@Testable
class JWTTest {
    val context = describe("JWT Support") {
        val restaurant = autoClose(Restaurant {
            jwt(JWTConfig.makeJwtVerifier()) {
                post("/handlers/reverser", ReverserService())
            }
        })
        it("allows authorized requests") {
            val response = request(restaurant, "/handlers/reverser") {
                post("""jakob""".toRequestBody())
                addHeader("Authorization", "Bearer ${JWTConfig.makeToken(1)}")
            }
            expectThat(response) {
                get { code }.isEqualTo(201)
                get { body }.isNotNull().get { string() }.isEqualTo("bokaj")
            }
        }

        it("returns 401 for unauthorized requests") {
            val response = request(restaurant, "/handlers/reverser") {
                post("""jakob""".toRequestBody())
            }
            expectThat(response) {
                get { code }.isEqualTo(401)
            }
        }
    }
}

