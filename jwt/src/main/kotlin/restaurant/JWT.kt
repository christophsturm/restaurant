package restaurant

import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier

fun RoutingDSL.jwt(verifier: JWTVerifier, function: RoutingDSL.() -> Unit) = wrap(JWTWrapper(verifier), function)

class JWTWrapper(private val verifier: JWTVerifier) : Wrapper {
    companion object DecodedJWTKey : Key<DecodedJWT>

    override suspend fun invoke(request: Request): WrapperResult {
        try {
            val token = request.headers["Authorization"]?.singleOrNull()?.substringAfter("Bearer ")
                ?: return FinishRequest(response(HttpStatus.UNAUTHORIZED_401, "Unauthorized: Auth Header not found"))
            return AddRequestConstant(DecodedJWTKey, verifier.verify(token))
        } catch (e: JWTVerificationException) {
            return FinishRequest(StringResponse(HttpStatus.UNAUTHORIZED_401, "Unauthorized: " + e.message))
        }
    }

}
