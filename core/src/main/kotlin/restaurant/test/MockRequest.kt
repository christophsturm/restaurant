package restaurant.test

import restaurant.HeaderMap
import restaurant.Key
import restaurant.Request
import restaurant.RequestContext
import java.util.Deque

class MockRequest(
    private val body: String? = null,
    override val queryParameters: Map<String, Deque<String>> = emptyMap()
) : Request {
    override val requestPath: String
        get() = TODO("Not yet implemented")
    override val queryString: String
        get() = TODO("Not yet implemented")
    override val headers: HeaderMap
        get() = TODO("Not yet implemented")

    override suspend fun readBody(): ByteArray = body?.toByteArray() ?: byteArrayOf()

}

fun RequestContext() = object : RequestContext {
    override fun <T> get(key: Key<T>): T {
        TODO("Not yet implemented")
    }
}
