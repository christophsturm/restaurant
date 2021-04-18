package fundertow.internal

import fundertow.HttpService
import fundertow.RestService

class RestServiceHandler(service: RestService) : HttpService {
    override fun handle(requestBody: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }
}
