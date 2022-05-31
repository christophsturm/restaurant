package restaurant

import kotlinx.coroutines.flow.Flow
import java.nio.ByteBuffer

fun response(status: Int = 200, headers: Map<String, String> = mapOf()) = StatusResponse(status, headers)
fun response(status: Int, result: String, headers: Map<String, String> = mapOf()) =
    StringResponse(status, result, headers)

fun response(status: Int, result: ByteBuffer, headers: Map<String, String> = mapOf()) =
    ByteBufferResponse(status, result, headers)

fun response(status: Int, result: ByteArray, headers: Map<String, String> = mapOf()) =
    ByteBufferResponse(status, ByteBuffer.wrap(result), headers)

fun response(result: ByteBuffer, headers: Map<String, String> = mapOf()) = ByteBufferResponse(200, result, headers)
fun response(result: ByteArray, headers: Map<String, String> = mapOf()) =
    ByteBufferResponse(200, ByteBuffer.wrap(result), headers)

fun response(result: String, headers: Map<String, String> = mapOf()) = StringResponse(200, result, headers)
sealed interface Response {
    val headers: Map<String, String>
    val status: Int
    fun bodyString(): String
}

data class StatusResponse(override val status: Int, override val headers: Map<String, String> = mapOf()) : Response {
    override fun bodyString() = ""
}

data class StringResponse(override val status: Int,
    val body: String,
    override val headers: Map<String, String> = mapOf()
) : Response {
    override fun bodyString() = body
}

data class ByteBufferResponse(
    override val status: Int,
    val body: ByteBuffer,
    override val headers: Map<String, String> = mapOf()
) : Response {
    override fun bodyString(): String {
        return String(body.array())
    }
}
data class FlowResponse(override val headers: Map<String, String>, override val status: Int, val body: Flow<String>) : Response {
    override fun bodyString() = "<FLOW>"
}
