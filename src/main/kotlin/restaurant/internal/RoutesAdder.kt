package restaurant.internal

import com.fasterxml.jackson.databind.ObjectMapper
import restaurant.HttpService
import restaurant.RestService

class RoutesAdder(private val objectMapper: ObjectMapper) {
    fun routesFor(handler: RestService): Routes {
        return Routes(RestServiceHandler(handler, objectMapper))
    }
}

data class Routes(val postRoute: HttpService)
