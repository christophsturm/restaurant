package restaurant

import java.nio.ByteBuffer

fun response(status: Int = 200, headers: Map<String, String> = mapOf()) = StatusResponse(status, headers)
fun response(status: Int, result: String, headers: Map<String, String> = mapOf()) =
    StringResponse(status, result, headers)

fun response(status: Int, result: ByteBuffer, headers: Map<String, String> = mapOf()) =
    ByteBufferResponse(status, result, headers)

fun response(result: ByteBuffer, headers: Map<String, String> = mapOf()) = ByteBufferResponse(200, result, headers)
fun response(result: String, headers: Map<String, String> = mapOf()) = StringResponse(200, result, headers)
sealed class Response {
    abstract val headers: Map<String, String>
    abstract val status: Int
}

data class StatusResponse(override val status: Int, override val headers: Map<String, String> = mapOf()) : Response()
data class StringResponse(
    override val status: Int,
    val result: String,
    override val headers: Map<String, String> = mapOf()
) : Response()

data class ByteBufferResponse(
    override val status: Int,
    val result: ByteBuffer,
    override val headers: Map<String, String> = mapOf()
) : Response()
