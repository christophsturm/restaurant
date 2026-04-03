package restaurant

data class RunningRestaurantServer(val server: RestaurantServer, val port: Int)

fun interface RestaurantServer : AutoCloseable {
    override fun close()
}

interface RestaurantServerFactory {
    val name: String

    fun start(
        rootHandlers: List<Pair<SuspendingHandler, Route>>,
        defaultHandler: SuspendingHandler,
        port: Int?,
        host: String
    ): RunningRestaurantServer
}
