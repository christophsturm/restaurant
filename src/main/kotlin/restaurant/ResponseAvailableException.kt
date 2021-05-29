package restaurant

class ResponseAvailableException(val status: Int, val body: String) : RuntimeException()
