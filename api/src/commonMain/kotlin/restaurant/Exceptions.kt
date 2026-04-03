package restaurant

open class ResponseException(val response: Response, message: String?) : RuntimeException(message) {
    override fun toString(): String {
        return "ResponseException($response)"
    }
}
