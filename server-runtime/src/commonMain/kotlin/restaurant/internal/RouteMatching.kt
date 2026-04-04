package restaurant.internal

import restaurant.ExceptionHandler
import restaurant.HttpStatus.INTERNAL_SERVER_ERROR_500
import restaurant.Method
import restaurant.Request
import restaurant.RequestWithBody
import restaurant.Route
import restaurant.RoutingDSL
import restaurant.SuspendingHandler
import restaurant.Wrapper
import restaurant.response

fun buildRoutes(serviceMapping: RoutingDSL.() -> Unit): List<Route> =
    CommonRouting("").apply(serviceMapping).routes

fun prepareRootHandlers(
    routes: List<Route>,
    exceptionHandler: ExceptionHandler
): List<Pair<SuspendingHandler, Route>> =
    routes.map { route ->
        createRootHandler(route.wrappers, exceptionHandler, route.handler) to route
    }

fun createRootHandler(
    wrappers: List<Wrapper>,
    exceptionHandler: ExceptionHandler,
    handler: SuspendingHandler
): SuspendingHandler {
    val wrappedHandler = wrappers.reversed().fold(handler) { acc, wrapper -> wrapper.wrap(acc) }
    return SuspendingHandler { request, requestContext ->
        try {
            wrappedHandler.handle(request, requestContext)
        } catch (e: Exception) {
            try {
                exceptionHandler(e)
            } catch (handlerException: Exception) {
                response(
                    INTERNAL_SERVER_ERROR_500,
                    "error in error handler" + handlerException.toString())
            }
        }
    }
}

data class MatchedHandler(val handler: SuspendingHandler, val pathParameters: Map<String, String>)

fun findRoute(
    rootHandlers: List<Pair<SuspendingHandler, Route>>,
    requestMethod: Method,
    requestPath: String
): MatchedHandler? {
    val matchedRoute =
        rootHandlers
            .asSequence()
            .mapNotNull { (handler, route) ->
                if (route.method != requestMethod) return@mapNotNull null
                val pathMatch = matchPath(route.path, requestPath) ?: return@mapNotNull null
                MatchedRoute(handler, pathMatch.pathParameters, pathMatch.literalSegmentCount)
            }
            .maxByOrNull { it.literalSegmentCount } ?: return null
    return MatchedHandler(matchedRoute.handler, matchedRoute.pathParameters)
}

fun Request.withPathParameters(pathParameters: Map<String, String>): Request {
    if (pathParameters.isEmpty()) return this
    return RequestWithPathParameters(this, mergeQueryParameters(queryParameters, pathParameters))
}

private data class MatchedRoute(
    val handler: SuspendingHandler,
    val pathParameters: Map<String, String>,
    val literalSegmentCount: Int
)

private data class PathMatch(val pathParameters: Map<String, String>, val literalSegmentCount: Int)

private fun matchPath(routePath: String, requestPath: String): PathMatch? {
    val routeSegments = normalizePath(routePath).splitIntoSegments()
    val requestSegments = normalizePath(requestPath).splitIntoSegments()
    if (routeSegments.size != requestSegments.size) return null
    val pathParameters = mutableMapOf<String, String>()
    var literalSegmentCount = 0
    routeSegments.zip(requestSegments).forEach { (routeSegment, requestSegment) ->
        if (routeSegment.isPathParameter()) {
            pathParameters[routeSegment.removeSurrounding("{", "}")] = requestSegment
        } else if (routeSegment != requestSegment) {
            return null
        } else {
            literalSegmentCount++
        }
    }
    return PathMatch(pathParameters, literalSegmentCount)
}

private fun mergeQueryParameters(
    queryParameters: Map<String, Collection<String>>,
    pathParameters: Map<String, String>
): Map<String, Collection<String>> {
    val merged = queryParameters.toMutableMap()
    pathParameters.forEach { (name, value) -> merged[name] = (merged[name] ?: emptyList()) + value }
    return merged.toMap()
}

private fun String.isPathParameter(): Boolean = startsWith("{") && endsWith("}")

private fun normalizePath(path: String): String {
    val trimmed = path.trim()
    if (trimmed.isEmpty() || trimmed == "/") return "/"
    return "/" + trimmed.trim('/')
}

private fun String.splitIntoSegments(): List<String> =
    if (this == "/") emptyList() else trim('/').split('/')

private class CommonRouting(private val prefix: String) : RoutingDSL {
    val routes = mutableListOf<Route>()

    override fun route(method: Method, path: String, service: SuspendingHandler) {
        routes.add(Route(method, prefix + path, service))
    }

    override fun wrap(wrapper: Wrapper, function: RoutingDSL.() -> Unit) {
        val nested = CommonRouting(prefix)
        nested.function()
        routes += nested.routes.map { it.copy(wrappers = listOf(wrapper) + it.wrappers) }
    }

    override fun namespace(prefix: String, function: RoutingDSL.() -> Unit) {
        val routing = CommonRouting(this.prefix + prefix + "/")
        routing.function()
        routes += routing.routes
    }
}

private class RequestWithPathParameters(
    private val delegateRequest: Request,
    override val queryParameters: Map<String, Collection<String>>
) : Request by delegateRequest {
    private var requestWithBody: RequestWithBody? = null

    override suspend fun withBody(): RequestWithBody {
        if (requestWithBody != null) return requestWithBody!!
        requestWithBody = RequestWithPathParametersWithBody(this, delegateRequest.withBody().body)
        return requestWithBody!!
    }
}

private class RequestWithPathParametersWithBody(
    private val request: RequestWithPathParameters,
    override val body: ByteArray?
) : RequestWithBody, Request by request {
    override suspend fun withBody(): RequestWithBody = this
}
