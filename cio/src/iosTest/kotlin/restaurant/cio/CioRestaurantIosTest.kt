package restaurant.cio

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setHTTPBody
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import restaurant.EmbeddedRestaurant
import restaurant.Method
import restaurant.response

class CioRestaurantIosTest {
    @Test
    fun serves_real_http_requests_over_ios_loopback() = runBlocking {
        val restaurant =
            EmbeddedRestaurant(serverFactory = CioRestaurantServerFactory()) {
                route(Method.GET, "/ping") { _, _ -> response("pong") }
                route(Method.POST, "/echo") { request, _ ->
                    response(request.withBody().body!!.decodeToString())
                }
            }

        try {
            val getResponse = executeRequest("${restaurant.baseUrl}/ping")
            assertEquals(200, getResponse.status)
            assertEquals("pong", getResponse.body)

            val postResponse =
                executeRequest(
                    url = "${restaurant.baseUrl}/echo", method = "POST", body = "cio-on-ios")
            assertEquals(200, postResponse.status)
            assertEquals("cio-on-ios", postResponse.body)
        } finally {
            restaurant.close()
        }
    }
}

private data class HttpResponse(val status: Int, val body: String)

private suspend fun executeRequest(
    url: String,
    method: String = "GET",
    body: String? = null
): HttpResponse = suspendCancellableCoroutine { continuation ->
    val request =
        NSMutableURLRequest(uRL = checkNotNull(NSURL(string = url)) { "Invalid URL: $url" })
    request.setHTTPMethod(method)
    if (body != null) {
        request.setValue("text/plain; charset=utf-8", forHTTPHeaderField = "Content-Type")
        request.setHTTPBody(body.encodeToByteArray().toNSData())
    }

    val task =
        NSURLSession.sharedSession.dataTaskWithRequest(
            request = request,
            completionHandler = { data: NSData?, response: NSURLResponse?, error: NSError? ->
                if (error != null) {
                    continuation.resumeWithException(
                        IllegalStateException(error.localizedDescription))
                    return@dataTaskWithRequest
                }
                val httpResponse =
                    response as? NSHTTPURLResponse
                        ?: run {
                            continuation.resumeWithException(
                                IllegalStateException("Expected HTTP response"))
                            return@dataTaskWithRequest
                        }
                val responseBody = data.decodeUtf8()
                continuation.resume(HttpResponse(httpResponse.statusCode.toInt(), responseBody))
            })
    continuation.invokeOnCancellation { task.cancel() }
    task.resume()
}

@OptIn(BetaInteropApi::class)
private fun NSData?.decodeUtf8(): String =
    if (this == null) "" else NSString.create(this, NSUTF8StringEncoding).toString()

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}
