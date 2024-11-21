package restaurant

import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier

fun RoutingDSL.jwt(verifier: JWTVerifier, function: RoutingDSL.() -> Unit) =
    wrap(JWTWrapper(verifier), function)

class JWTWrapper(private val verifier: JWTVerifier) : Wrapper {
    companion object DecodedJWTKey : Key<DecodedJWT>

    override fun wrap(wrapped: SuspendingHandler): SuspendingHandler {
        return SuspendingHandler { request, requestContext ->
            val token = request.headers["Authorization"]?.singleOrNull()?.substringAfter("Bearer ")
            if (token == null)
                response(HttpStatus.UNAUTHORIZED_401, "Unauthorized: Auth Header not found")
            else {
                requestContext.add(DecodedJWTKey, verifier.verify(token))
                wrapped.handle(request, requestContext)
            }
        }
    }
}
