package restaurant.netty

import restaurant.*
import restaurant.internal.netty.buildNetty

class NettyRestaurantServerFactory : RestaurantServerFactory {
    override val name: String = "netty"

    override fun start(
        rootHandlers: List<Pair<SuspendingHandler, Route>>,
        defaultHandler: SuspendingHandler,
        port: Int?,
        host: String
    ): RunningRestaurantServer = buildNetty(rootHandlers, defaultHandler, port, host)
}
