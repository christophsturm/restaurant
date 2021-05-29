package restaurant

import java.nio.ByteBuffer

fun response(status: Int = 200) = StatusResponse(status)
fun response(status: Int, result: String) = StringResponse(status, result)
fun response(status: Int, result: ByteBuffer) = ByteBufferResponse(status, result)
fun response(result: ByteBuffer) = ByteBufferResponse(200, result)
sealed class Response {
    abstract val status: Int
}

data class StatusResponse(override val status: Int) : Response()
data class StringResponse(override val status: Int, val result: String) : Response()
data class ByteBufferResponse(override val status: Int, val result: ByteBuffer) : Response()
