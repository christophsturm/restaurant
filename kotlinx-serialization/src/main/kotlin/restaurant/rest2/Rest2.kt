package restaurant.rest2

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import restaurant.*
import java.util.*

inline fun <Service : Any> RoutingDSL.resources(
    service: Service,
    path: String = ___path(service),
    config: ResourceMapper<Service>.() -> Unit
) =
    ResourceMapperImpl(this, service, path).config()

inline fun <Service : Any, ServiceResponse> RoutingDSL.resources(
    service: Service,
    responseSerializer: KSerializer<ServiceResponse>,
    path: String = ___path(service),
    config: ResourceMapperWithDefaultType<Service, ServiceResponse>.() -> Unit

) =
    ResourceMapperWithDefaultType(responseSerializer, ResourceMapperImpl(this, service, path)).config()

class ResourceMapperWithDefaultType<Service : Any, DefaultType>(
    private val responseSerializer: KSerializer<DefaultType>,
    private val resourceMapper: ResourceMapper<Service>
) : ResourceMapper<Service> by resourceMapper {
    fun show(body: suspend Service.(ShowContext) -> DefaultType) {
        resourceMapper.show(responseSerializer, body)
    }

    fun create(body: suspend Service.(CreateContext<DefaultType>) -> DefaultType) {
        resourceMapper.create(responseSerializer, body)
    }
}

fun ___path(service: Any) =
    service::class.simpleName!!.lowercase(Locale.getDefault()).removeSuffix("service") + "s"

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

    fun <ServiceResponse> index(
        responseSerializer: KSerializer<ServiceResponse>,
        body: suspend Service.() -> ServiceResponse
    )

    fun <RequestAndResponse> update(
        serializer: KSerializer<RequestAndResponse>,
        body: suspend Service.(UpdateContext<RequestAndResponse>) -> RequestAndResponse
    )

    fun <ServiceResponse> streamIndex(
        responseSerializer: KSerializer<ServiceResponse>,
        body: suspend Service.() -> Flow<ServiceResponse>
    )
}

class ResourceMapperImpl<Service : Any>(
    private val routingDSL: RoutingDSL,
    private val service: Service,
    private val path: String = ___path(service)
) : ResourceMapper<Service> {
    override fun <ServiceResponse> index(
        responseSerializer: KSerializer<ServiceResponse>,
        body: suspend Service.() -> ServiceResponse
    ) {
        routingDSL.route(Method.GET, path, IndexHandler(responseSerializer, service, body))
    }

    override fun <ServiceResponse> streamIndex(
        responseSerializer: KSerializer<ServiceResponse>,
        body: suspend Service.() -> Flow<ServiceResponse>
    ) {
        routingDSL.route(Method.GET, path, FlowIndexHandler(responseSerializer, service, body))
    }

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
            Method.POST, path, CreateHandler(
                requestSerializer, responseSerializer, service, body
            )
        )
    }

    override fun <RequestAndResponse> update(
        serializer: KSerializer<RequestAndResponse>,
        body: suspend Service.(UpdateContext<RequestAndResponse>) -> RequestAndResponse
    ) {
        routingDSL.route(
            Method.PUT,
            "$path/{id}",
            UpdateHandler(serializer, serializer, service, body)
        )
    }
}

interface HasBody<RequestType> {
    val body: RequestType
}

interface HasId {
    fun intId(): Int
}

interface CreateContext<RequestType> : HasBody<RequestType>
interface ShowContext : HasId
interface UpdateContext<RequestType> : HasBody<RequestType>, HasId
class IndexHandler<Service : Any, ServiceResponse>(
    private val responseSerializer: KSerializer<ServiceResponse>,
    private val service: Service,
    val function: (suspend Service.() -> ServiceResponse)
) : SuspendingHandler {
    override suspend fun handle(request: Request, requestContext: MutableRequestContext): Response {
        val result = service.function()
        return response(200, Json.encodeToString(responseSerializer, result))
    }
}

class ShowHandler<Service : Any, ServiceResponse>(
    private val responseSerializer: KSerializer<ServiceResponse>,
    private val service: Service,
    val function: (suspend Service.(ShowContext) -> ServiceResponse)
) : SuspendingHandler {
    override suspend fun handle(request: Request, requestContext: MutableRequestContext): Response {
        val id = request.queryParameters.let {
            it["id"]?.singleOrNull()
                ?: throw RuntimeException("id variable not found. variables: ${it.keys.joinToString()}")
        }
        val result = service.function(ShowContextImpl(id))
        return response(200, Json.encodeToString(responseSerializer, result))
    }
}

class CreateHandler<Service : Any, ServiceRequest, ServiceResponse>(
    private val requestSerializer: KSerializer<ServiceRequest>,
    private val responseSerializer: KSerializer<ServiceResponse>,
    private val service: Service,
    val function: (suspend Service.(CreateContext<ServiceRequest>) -> ServiceResponse)
) : SuspendingHandler {
    override suspend fun handle(request: Request, requestContext: MutableRequestContext): Response {
        val payload = request.withBody().body.let {
            val string = String(it!!)
            try {
                Json.decodeFromString(requestSerializer, string)
            } catch (e: Exception) {
                throw RestaurantException("error deserializing request body: $string", e)
            }
        }

        val result = service.function(CreateContextImpl(payload))
        return response(201, Json.encodeToString(responseSerializer, result))
    }
}

class UpdateHandler<Service : Any, ServiceRequest, ServiceResponse>(
    private val requestSerializer: KSerializer<ServiceRequest>,
    private val responseSerializer: KSerializer<ServiceResponse>,
    private val service: Service,
    val function: (suspend Service.(UpdateContext<ServiceRequest>) -> ServiceResponse)
) : SuspendingHandler {
    override suspend fun handle(request: Request, requestContext: MutableRequestContext): Response {
        val id = request.queryParameters.let {
            it["id"]?.singleOrNull()
                ?: throw RuntimeException("id variable not found. variables: ${it.keys.joinToString()}")
        }
        val payload = request.withBody().body.let {
            val string = String(it!!)
            try {
                Json.decodeFromString(requestSerializer, string)
            } catch (e: Exception) {
                throw RestaurantException("error deserializing request body: $string", e)
            }
        }

        val result = service.function(UpdateContextImpl(payload, id))
        return response(200, Json.encodeToString(responseSerializer, result))
    }
}

class CreateContextImpl<ServiceRequest>(override val body: ServiceRequest) : CreateContext<ServiceRequest>

class UpdateContextImpl<ServiceRequest>(override val body: ServiceRequest, private val id: String) :
    UpdateContext<ServiceRequest> {
    override fun intId(): Int = id.toInt()
}

class ShowContextImpl(private val id: String) : ShowContext {
    override fun intId(): Int {
        return id.toInt()
    }
}

class FlowIndexHandler<Service : Any, ServiceResponse>(
    private val responseSerializer: KSerializer<ServiceResponse>,
    private val service: Service,
    val function: (suspend Service.() -> Flow<ServiceResponse>)
) : SuspendingHandler {
    override suspend fun handle(request: Request, requestContext: MutableRequestContext): Response {
        val result = service.function()
        return FlowResponse(mapOf(), 200, result.map { Json.encodeToString(responseSerializer, it) + "\n" })
    }
}
