package restaurant

import failgood.ResourcesDSL
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

val client = okhttp3.OkHttpClient.Builder()
    .connectTimeout(1000, TimeUnit.MILLISECONDS)
    .readTimeout(1000, TimeUnit.MILLISECONDS)
    .build()

fun ResourcesDSL.request(
    restaurant: Restaurant,
    path: String,
    config: Request.Builder.() -> Request.Builder = { this }
): Response {
    return autoClose(
        client.newCall(Request.Builder().url("http://localhost:${restaurant.port}$path").config().build()).execute()
    )
}
