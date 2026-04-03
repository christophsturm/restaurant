package restaurant

import java.nio.ByteBuffer

fun response(status: Int, result: ByteBuffer, headers: Map<String, String> = mapOf()) =
    response(status, result.toByteArray(), headers)

fun response(result: ByteBuffer, headers: Map<String, String> = mapOf()) =
    response(result.toByteArray(), headers)

private fun ByteBuffer.toByteArray(): ByteArray {
    val copy = asReadOnlyBuffer()
    val bytes = ByteArray(copy.remaining())
    copy.get(bytes)
    return bytes
}
