package restaurant.undertow

import restaurant.*
import restaurant.internal.undertow.buildUndertow

class UndertowRestaurantServerFactory : RestaurantServerFactory {
    override val name: String = "undertow"

    override fun start(
        rootHandlers: List<Pair<SuspendingHandler, Route>>,
        defaultHandler: SuspendingHandler,
        port: Int?,
        host: String
    ): RunningRestaurantServer = buildUndertow(rootHandlers, defaultHandler, port, host)
}
