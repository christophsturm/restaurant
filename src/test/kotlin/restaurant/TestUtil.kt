package restaurant

import failfast.ContextDSL
import okhttp3.Request
import okhttp3.Response

val client = okhttp3.OkHttpClient()
fun ContextDSL.request(restaurant: Restaurant, path: String, config: Request.Builder.() -> Request.Builder = { this }): Response {
    return autoClose(
        client.newCall(
            Request.Builder().url("http://localhost:${restaurant.port}$path").config().build()
        ).execute()
    ) { it.close() }
}
