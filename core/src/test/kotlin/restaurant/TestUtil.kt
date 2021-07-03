package restaurant

import failgood.ResourcesDSL
import kotlinx.coroutines.future.await
import okhttp3.Request
import okhttp3.Response
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.TimeUnit

val client = okhttp3.OkHttpClient.Builder()
    .connectTimeout(1000, TimeUnit.MILLISECONDS)
    .readTimeout(1000, TimeUnit.MILLISECONDS)
    .build()

val httpClient = HttpClient.newHttpClient()!!

fun ResourcesDSL.request(
    restaurant: Restaurant,
    path: String,
    config: Request.Builder.() -> Request.Builder = { this }
): Response {
    return autoClose(
        client.newCall(Request.Builder().url("http://localhost:${restaurant.port}$path").config().build()).execute()
    )
}

suspend fun req(
    restaurant: Restaurant,
    path: String,
    config: HttpRequest.Builder.() -> HttpRequest.Builder = { this }
): HttpResponse<String> {
    val request =
        HttpRequest.newBuilder(URI("http://localhost:${restaurant.port}$path")).timeout(Duration.ofSeconds(1))
            .config()
            .build()
    return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()!!
}

