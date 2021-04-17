package fundertow

import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange

class RestServiceHandler(service: fundertow.RestService) : HttpHandler {
    override fun handleRequest(exchange: HttpServerExchange) {
        TODO("Not yet implemented")
    }

}
