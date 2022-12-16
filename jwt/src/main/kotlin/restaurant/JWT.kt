package restaurant

import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier

fun RoutingDSL.jwt(verifier: JWTVerifier, function: RoutingDSL.() -> Unit) = wrap(RealJWTWrapper(verifier), function)

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

class RealJWTWrapper(private val verifier: JWTVerifier) : RealWrapper {
    override fun wrap(wrapped: SuspendingHandler): SuspendingHandler {
        return SuspendingHandler { request, requestContext ->
            val token = request.headers["Authorization"]?.singleOrNull()?.substringAfter("Bearer ")
            if (token == null)
                response(HttpStatus.UNAUTHORIZED_401, "Unauthorized: Auth Header not found")
            else {
                requestContext.add(JWTWrapper.DecodedJWTKey, verifier.verify(token))
                wrapped.handle(request, requestContext)
            }
        }
    }
}
