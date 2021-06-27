package restaurant.internal

interface Mapper {
    fun <T:Any> readValue(requestBody: ByteArray?, clazz: Class<T>): T
    fun writeValueAsBytes(value: Any?): ByteArray
}
