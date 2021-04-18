package fundertow.internal

import failfast.describe
import fundertow.HttpService
import fundertow.UserService

object RestServiceHandlerTest {
    val context = describe(RestServiceHandler::class) {
        it("wraps a Rest Service") {
            val handler: HttpService = RestServiceHandler(UserService())
        }
    }
}
