package restaurant

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import failgood.describe
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

class JWTWelcomeHandler : SuspendingHandler {
    override suspend fun handle(exchange: Exchange, requestContext: RequestContext): Response {
        return (response("welcome user " + requestContext[JWTWrapper].getClaim("jti")))
    }
}

@Testable
class JWTTest {
    val context = describe("JWT Support") {
        val restaurant = autoClose(restaurant {
            jwt(JWTConfig.makeJwtVerifier()) {
                route(Method.GET, "/handlers/welcome", JWTWelcomeHandler())
            }
        })
        it("allows authorized requests") {
            val response = request(restaurant, "/handlers/welcome") {
                addHeader("Authorization", "Bearer ${JWTConfig.makeToken(42)}")
            }
            expectThat(response) {
                get { code }.isEqualTo(200)
                get { body }.isNotNull().get { string() }.isEqualTo("welcome user 42")
            }
        }

        it("returns 401 for unauthorized requests") {
            val response = request(restaurant, "/handlers/welcome")
            expectThat(response) {
                get { code }.isEqualTo(401)
            }
        }
    }
}

