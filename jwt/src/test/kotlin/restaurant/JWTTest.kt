package restaurant

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.JWTVerifier
import failgood.Test
import failgood.testsAbout
import strikt.api.expectThat
import strikt.assertions.isEqualTo

object JWTConfig {
    private const val ISSUER = "https://restaurant.dev/"
    private const val AUDIENCE = "jwtAudience"
    private val algorithm = Algorithm.HMAC256("psst, secret")!!
    fun makeToken(userId: Long): String = JWT.create()
        .withAudience(AUDIENCE)
        .withSubject("Authentication")
        .withIssuer(ISSUER)
        .withClaim("jti", userId)
        .sign(algorithm)!!

    fun makeJwtVerifier(): JWTVerifier = JWT.require(algorithm)
        .withAudience(AUDIENCE)
        .withIssuer(ISSUER)
        .build()
}

class JWTWelcomeHandler : SuspendingHandler {
    override suspend fun handle(request: Request, requestContext: MutableRequestContext): Response {
        return (response("welcome user " + requestContext[JWTWrapper].getClaim("jti")))
    }
}

@Test
class JWTTest {
    val context = testsAbout("JWT Support") {
        val restaurant = autoClose(
            Restaurant {
                jwt(JWTConfig.makeJwtVerifier()) {
                    route(Method.GET, "/handlers/welcome", JWTWelcomeHandler())
                }
            }
        )
        it("allows authorized requests") {
            val response = restaurant.sendRequest("/handlers/welcome") {
                addHeader("Authorization", "Bearer ${JWTConfig.makeToken(42)}")
            }
            expectThat(response) {
                get { statusCode() }.isEqualTo(200)
                get { body() }.isEqualTo("welcome user 42")
            }
        }

        it("returns 401 for unauthorized requests") {
            val response = restaurant.sendRequest("/handlers/welcome")
            expectThat(response) {
                get { statusCode() }.isEqualTo(401)
            }
        }
    }
}
