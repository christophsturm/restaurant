package restaurant.test

import restaurant.HeaderMap
import restaurant.Key
import restaurant.Method
import restaurant.RequestContext
import restaurant.RequestWithBody
import java.util.Deque

class MockRequest(
    override val body: ByteArray? = null,
    override val queryParameters: Map<String, Deque<String>> = emptyMap()
) : RequestWithBody {
    override val requestPath: String
        get() = TODO("Not yet implemented")
    override val queryString: String
        get() = TODO("Not yet implemented")
    override val headers: HeaderMap
        get() = TODO("Not yet implemented")
    override val method: Method
        get() = TODO("Not yet implemented")

    override suspend fun withBody() = this
}

fun RequestContext() = object : RequestContext {
    override fun <T> get(key: Key<T>): T {
        TODO("Not yet implemented")
    }
}
