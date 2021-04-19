package restaurant.internal

import com.fasterxml.jackson.databind.ObjectMapper
import restaurant.HttpService
import restaurant.RestService
import kotlin.reflect.full.functions

class RoutesAdder(private val objectMapper: ObjectMapper) {
    fun routesFor(restService: RestService): Routes {
        val functions = restService::class.functions.associateBy { it.name }
        val postHandler = RestServiceHandler(
            restService,
            objectMapper,
            functions["create"]
        )
        return Routes(postHandler)
    }
}

data class Routes(val postRoute: HttpService?)
