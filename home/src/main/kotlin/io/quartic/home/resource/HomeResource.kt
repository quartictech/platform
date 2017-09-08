package io.quartic.home.resource

import io.dropwizard.auth.Auth
import io.quartic.catalogue.api.CatalogueService
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetCoordinates
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.api.model.DatasetLocator.CloudDatasetLocator
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.common.auth.User
import io.quartic.common.geojson.GeoJsonParser
import io.quartic.common.logging.logger
import io.quartic.eval.api.EvalQueryServiceClient
import io.quartic.eval.api.EvalTriggerServiceClient
import io.quartic.eval.api.model.BuildTrigger
import io.quartic.home.CreateDatasetRequest
import io.quartic.home.CreateStaticDatasetRequest
import io.quartic.home.FileType
import io.quartic.home.FileType.*
import io.quartic.home.conversion.CsvConverter
import io.quartic.howl.api.HowlService
import io.quartic.howl.api.HowlStorageId
import io.quartic.registry.api.RegistryServiceClient
import org.apache.commons.io.IOUtils.copy
import retrofit2.HttpException
import java.io.IOException
import java.io.InputStream
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import javax.annotation.security.PermitAll
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@PermitAll
@Path("/")
class HomeResource(
    private val catalogue: CatalogueService,
    private val howl: HowlService,
    private val evalQuery: EvalQueryServiceClient,
    private val evalTrigger: EvalTriggerServiceClient,
    private val registry: RegistryServiceClient
) {
    private val LOG by logger()
    // TODO - frontend will need to cope with DatasetNamespace in request paths and GET /datasets response

    // TODO - how does frontend know what namespace to use for dataset creation?
    @GET
    @Path("/dag")
    @Produces(MediaType.APPLICATION_JSON)
    fun getLatestDag(@Auth user: User) = evalQuery.getDagAsync(user.customerId!!)
        .wrapNotFound("DAG", "latest")

    @POST
    @Path("/build")
    fun build(@Auth user: User) = evalTrigger.triggerAsync(BuildTrigger.Manual(
        user = user.name,
        customerId = user.customerId!!,
        branch = "develop",
        triggerType = BuildTrigger.TriggerType.EXECUTE,
        timestamp = Instant.now()
    )).join()

    @GET
    @Path("/builds")
    @Produces(MediaType.APPLICATION_JSON)
    fun getBuilds(@Auth user: User) = evalQuery.getBuildsAsync(user.customerId!!)
        .wrapNotFound("Builds", user.customerId!!)

    @GET
    @Path("/dag/{build}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getDag(@Auth user: User, @PathParam("build") build: Long) = evalQuery.getDagAsync(user.customerId!!, build)
        .wrapNotFound("DAG", build)

    @GET
    @Path("/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    fun getDatasets(@Auth user: User): Map<DatasetNamespace, Map<DatasetId, DatasetConfig>> {
        if (user.customerId == null) {
            return emptyMap()
        } else {
            val customer = registry.getCustomerByIdAsync(user.customerId!!).get()
            return catalogue.getDatasets().filterKeys { namespace -> customer.namespace == namespace.namespace }
        }
    }


    @DELETE
    @Path("/datasets/{namespace}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun deleteDataset(
        @Auth user: User,
        @PathParam("namespace") namespace: DatasetNamespace,
        @PathParam("id") id: DatasetId
    ) {
        // Note there's a potential race-condition here - another catalogue client could have manipulated the
        // dataset in-between these two statements.  It shouldn't matter - we will never delete a dataset not in an
        // authorised namespace.
        throwIfDatasetNotPresentOrNotAllowed(user, DatasetCoordinates(namespace, id))
        catalogue.deleteDataset(namespace, id)
    }

    @POST
    @Path("/datasets/{namespace}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun createDataset(
        @Auth user: User,
        @PathParam("namespace") namespace: DatasetNamespace,
        request: CreateDatasetRequest
    ): DatasetId {
        throwIfNamespaceNotAllowed(user, namespace)
        val datasetConfig = when (request) {
            is CreateStaticDatasetRequest -> {
                try {
                    val name = preprocessFile(namespace.namespace, request.fileName, request.fileType)
                    val locator = CloudDatasetLocator(
                        "/%s/%s".format(namespace, name),
                        false,
                        request.mimeType())
                    DatasetConfig(
                        request.metadata,
                        locator,
                        request.extensions()
                    )
                } catch (e: IOException) {
                    throw RuntimeException("Exception while preprocessing file", e)
                }
            }
            else -> throw BadRequestException("Unknown request type '${request.javaClass.simpleName}'")
        }

        return catalogue.registerDataset(namespace, datasetConfig).id
    }

    private fun preprocessFile(namespace: String, fileName: String, fileType: FileType): String {
        val stream: InputStream = howl.downloadFile(namespace, fileName)
                ?: throw NotFoundException("File not found: " + fileName)

        return stream.use { s ->
            when (fileType) {
                GEOJSON -> {
                    try {
                        GeoJsonParser(s).validate()
                    } catch (e: Exception) {
                        throw BadRequestException("Exception while validating GeoJSON", e)
                    }
                    fileName
                }
                CSV -> {
                    val storageId = howl.uploadAnonymousFile(namespace, MediaType.APPLICATION_JSON) { outputStream ->
                        try {
                            CsvConverter().convert(s, outputStream)
                        } catch (e: IOException) {
                            throw BadRequestException("Exception while converting CSV to GeoJSON", e)
                        }
                    }
                    storageId.uid
                }
                RAW -> fileName
            }
        }
    }

    @POST
    @Path("/file/{namespace}")
    @Produces(MediaType.APPLICATION_JSON)
    fun uploadFile(
        @PathParam("namespace") namespace: DatasetNamespace,
        @Context request: HttpServletRequest): HowlStorageId {
        return howl.uploadAnonymousFile(namespace.namespace, request.contentType) { outputStream ->
            try {
                copy(request.inputStream, outputStream)
            } catch (e: Exception) {
                throw RuntimeException("Exception while uploading file: " + e)
            }
        }
    }

    private fun <T> CompletableFuture<T>.wrapNotFound(type: String, name: Any): T = try {
        get()
    } catch (e: ExecutionException) {
        val cause = e.cause
        if (cause is HttpException && cause.code() == 404) {
            throw notFoundException(type, name)
        } else {
            throw cause!!
        }
    }

    private fun notFoundException(type: String, name: Any) = NotFoundException("$type '$name' not found")

    private fun throwIfDatasetNotPresentOrNotAllowed(user: User, coords: DatasetCoordinates) {
        val datasets = getDatasets(user)    // These will already be filtered to those that the user is authorised for

        val datasetsInNamespace = datasets[coords.namespace]
                ?: throw notFoundException("Namespace", coords.namespace.namespace)
        if (!datasetsInNamespace.contains(coords.id)) {
            throw notFoundException("Dataset", coords.id.uid)
        }
    }

    private fun throwIfNamespaceNotAllowed(user: User, namespace: DatasetNamespace) {
        val customer = registry.getCustomerByIdAsync(user.customerId!!).get()
        if (customer.namespace != namespace.namespace) {
            throw notFoundException("Namespace", namespace.namespace) // 404 instead of 403 to prevent discovery
        }
    }
}
