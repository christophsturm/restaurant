package restaurant

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import failfast.describe
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.platform.commons.annotation.Testable
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

@Testable
class JWTTest {
    val context = describe("JWT Support") {
        val restaurant = autoClose(Restaurant {
            jwt {
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

        pending("returns 401 for unauthorized requests") {
            val response = request(restaurant, "/handlers/reverser") {
                post("""jakob""".toRequestBody())
            }
            expectThat(response) {
                get { code }.isEqualTo(401)
            }

        }

    }
}

object JWTConfig {
    private const val issuer = "https://the.io/restaurant"
    const val audience = "jwtAudience"
    const val realm = "rest-test"
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
