package restaurant

import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier
import io.undertow.util.Headers

fun RoutingDSL.jwt(verifier: JWTVerifier, function: RoutingDSL.() -> Unit) {
    wrap(JWTWrapper(verifier), function)
}


class JWTWrapper(private val verifier: JWTVerifier) : Wrapper {
    companion object DecodedJWTKey : Key<DecodedJWT>

    override suspend fun invoke(exchange: Exchange): WrapperResult {
        try {
            val token = exchange.headers[Headers.AUTHORIZATION]?.singleOrNull()?.substringAfter("Bearer ")
                ?: return FinishRequest(StringResponse(401, "Unauthorized: Auth Header not found"))
            return AddRequestConstant(DecodedJWTKey, verifier.verify(token))
        } catch (e: JWTVerificationException) {
            return FinishRequest(StringResponse(401, "Unauthorized: " + e.message))
        }
    }

}
