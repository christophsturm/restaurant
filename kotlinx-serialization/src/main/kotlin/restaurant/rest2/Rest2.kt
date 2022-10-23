package restaurant.rest2

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import restaurant.Method
import restaurant.Request
import restaurant.RequestContext
import restaurant.Response
import restaurant.RestaurantException
import restaurant.RoutingDSL
import restaurant.SuspendingHandler
import restaurant.response
import java.util.Locale
import kotlin.reflect.KClass

fun <Service : Any> RoutingDSL.resources(service: Service, path: String = path(service)) =
    ResourceMapperImpl(this, service, path)

fun <Service : Any, ServiceResponse> RoutingDSL.resources(
    service: Service,
    responseSerializer: KSerializer<ServiceResponse>,
    path: String = path(service)
) =
    ResourceMapperWithDefaultType(responseSerializer, ResourceMapperImpl(this, service, path))

class ResourceMapperWithDefaultType<Service : Any, DefaultType>(
    private val responseSerializer: KSerializer<DefaultType>,
    private val resourceMapper: ResourceMapper<Service>
) : ResourceMapper<Service> by resourceMapper {
    fun show(body: suspend Service.(ShowContext) -> DefaultType) {
        resourceMapper.show(responseSerializer, body)
    }
    fun create(
        body: suspend Service.(CreateContext<DefaultType>) -> DefaultType
    ) {
        resourceMapper.create(responseSerializer, body)
    }
}

@Suppress("UNUSED_PARAMETER")
interface Context {
    fun <T : Any> get(kClass: KClass<T>): T {
        TODO()
    }

    fun id(): String
    fun intId(): Int
}

private inline fun <reified T : Any> Context.body(): T {
    return this.get(T::class)
}

private fun path(service: Any) =
    service::class.simpleName!!.lowercase(Locale.getDefault()).removeSuffix("service")

interface ResourceMapper<Service : Any> {
    fun <ServiceResponse> show(
        responseSerializer: KSerializer<ServiceResponse>,
        body: suspend Service.(ShowContext) -> ServiceResponse
    )

    fun <RequestAndResponse> create(
        serializer: KSerializer<RequestAndResponse>,
        body: suspend Service.(CreateContext<RequestAndResponse>) -> RequestAndResponse
    )

    fun <Request, Response> create(
        requestSerializer: KSerializer<Request>,
        responseSerializer: KSerializer<Response>,
        body: suspend Service.(CreateContext<Request>) -> Response
    )
}

@Suppress("UNUSED_PARAMETER")
class ResourceMapperImpl<Service : Any>(
    private val routingDSL: RoutingDSL,
    private val service: Service,
    private val path: String = path(service)
) : ResourceMapper<Service> {
    override fun <ServiceResponse> show(
        responseSerializer: KSerializer<ServiceResponse>,
        body: suspend Service.(ShowContext) -> ServiceResponse
    ) {
        routingDSL.route(Method.GET, "$path/{id}", ShowHandler(responseSerializer, service, body))
    }

    override fun <RequestAndResponse> create(
        serializer: KSerializer<RequestAndResponse>,
        body: suspend Service.(CreateContext<RequestAndResponse>) -> RequestAndResponse
    ) {
        routingDSL.route(
            Method.POST,
            path,
            CreateHandler(serializer, serializer, service, body)
        )
    }
    override fun <Request, Response> create(
        requestSerializer: KSerializer<Request>,
        responseSerializer: KSerializer<Response>,
        body: suspend Service.(CreateContext<Request>) -> Response
    ) {
        routingDSL.route(
            Method.POST,
            path,
            CreateHandler(requestSerializer, responseSerializer, service, body)
        )
    }
}

interface CreateContext<ResponseType> {
    val body: ResponseType
}

interface ShowContext {
    fun intId(): Int
}

class ShowHandler<Service : Any, ServiceResponse>(
    private val responseSerializer: KSerializer<ServiceResponse>,
    private val service: Service,
    val show: (suspend Service.(ShowContext) -> ServiceResponse)
) : SuspendingHandler {
    override suspend fun handle(request: Request, requestContext: RequestContext): Response {
        val id = request.queryParameters.let {
            it["id"]?.singleOrNull()
                ?: throw RuntimeException("id variable not found. variables: ${it.keys.joinToString()}")
        }
        val result = service.show(ShowContextImpl(id))
        return response(200, Json.encodeToString(responseSerializer, result))
    }
}

class CreateHandler<Service : Any, ServiceRequest, ServiceResponse>(
    private val requestSerializer: KSerializer<ServiceRequest>,
    private val responseSerializer: KSerializer<ServiceResponse>,
    private val service: Service,
    val show: (suspend Service.(CreateContext<ServiceRequest>) -> ServiceResponse)
) : SuspendingHandler {
    override suspend fun handle(request: Request, requestContext: RequestContext): Response {
        val payload = request.withBody().body.let {
            val string = String(it!!)
            try {
                Json.decodeFromString(requestSerializer, string)
            } catch (e: Exception) {
                throw RestaurantException("error deserializing request body: $string")
            }
        }

        val result = service.show(CreateContextImpl(payload))
        return response(201, Json.encodeToString(responseSerializer, result))
    }
}

class CreateContextImpl<ServiceRequest>(override val body: ServiceRequest) : CreateContext<ServiceRequest>

class ShowContextImpl(private val id: String) : ShowContext {
    override fun intId(): Int {
        return id.toInt()
    }
}
