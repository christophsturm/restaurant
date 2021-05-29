package restaurant

import java.nio.ByteBuffer

fun response(status: Int) = StatusResponse(status)
fun response(status: Int, result: String) = StringResponse(status, result)
fun response(status: Int, result: ByteBuffer) = ByteBufferResponse(status, result)
sealed class Response {
    abstract val status: Int
}

data class StatusResponse(override val status: Int) : Response()
data class StringResponse(override val status: Int, val result: String) : Response()
data class ByteBufferResponse(override val status: Int, val result: ByteBuffer) : Response()
