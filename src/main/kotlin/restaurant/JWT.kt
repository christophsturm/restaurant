package restaurant

import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.JWTVerifier
import io.undertow.util.Headers

fun RoutingDSL.jwt(verifier: JWTVerifier, function: RoutingDSL.() -> Unit) {
    wrap(JWTWrapper(verifier), function)
}

class JWTWrapper(private val verifier: JWTVerifier) : Wrapper {
    override suspend fun invoke(exchange: Exchange): StringResponse? {
        try {
            val token = exchange.headers[Headers.AUTHORIZATION]?.singleOrNull()?.substringAfter("Bearer ")
                ?: return StringResponse(401, "Unauthorized: Auth Header not found")
            verifier.verify(token)
        } catch (e: JWTVerificationException) {
            return StringResponse(401, "Unauthorized: " + e.message)
        }
        return null
    }

}
