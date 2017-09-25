package io.quartic.home.resource

import io.dropwizard.auth.Auth
import io.quartic.catalogue.api.CatalogueService
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.api.model.DatasetLocator.CloudDatasetLocator
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.common.auth.User
import io.quartic.eval.api.EvalQueryServiceClient
import io.quartic.eval.api.EvalTriggerServiceClient
import io.quartic.eval.api.model.*
import io.quartic.home.model.*
import io.quartic.howl.api.HowlService
import io.quartic.howl.api.HowlStorageId
import io.quartic.registry.api.RegistryServiceClient
import org.apache.commons.io.IOUtils.copy
import retrofit2.HttpException
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
    @GET
    @Path("/dag")
    @Produces(MediaType.APPLICATION_JSON)
    fun getLatestDag(@Auth user: User) = evalQuery.getLatestDagAsync(user.customerId!!)
        .wrapNotFound("DAG", "latest")
        .toCytoscape()

    @GET
    @Path("/dag/{build}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getDag(
        @Auth user: User,
        @PathParam("build") build: Long
    ) = evalQuery.getDagAsync(user.customerId!!, build)
        .wrapNotFound("DAG", build)
        .toCytoscape()

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
    @Path("/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    fun getDatasets(
        @Auth user: User
    ): Map<DatasetNamespace, Map<DatasetId, DatasetConfig>> {
        val namespace = lookupNamespace(user)
        return catalogue.getDatasets().filterKeys { namespace.namespace == it.namespace }
    }

    @DELETE
    @Path("/datasets/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun deleteDataset(
        @Auth user: User,
        @PathParam("id") id: DatasetId
    ) {
        // Note there's a potential race-condition here - another catalogue client could have manipulated the
        // dataset in-between the two catalogue calls.
        val namespace = lookupNamespace(user)
        val datasets = getDatasets(user)[namespace] ?: emptyMap()
        if (id !in datasets) {
            throw notFoundException("Dataset", id.uid)
        }
        catalogue.deleteDataset(namespace, id)
    }

    @POST
    @Path("/datasets")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun createDataset(
        @Auth user: User,
        request: CreateDatasetRequest
    ): DatasetId {
        val namespace = lookupNamespace(user)
        val datasetConfig = DatasetConfig(
            request.metadata,
            CloudDatasetLocator(
                "/${namespace}/${namespace}/${request.fileName}",
                false,
                DEFAULT_MIME_TYPE
            )
        )
        return catalogue.registerDataset(namespace, datasetConfig).id
    }

    @POST
    @Path("/file")
    @Produces(MediaType.APPLICATION_JSON)
    fun uploadFile(
        @Auth user: User,
        @Context request: HttpServletRequest
    ): HowlStorageId {
        val namespace = lookupNamespace(user)
        return howl.uploadAnonymousFile(namespace.namespace, namespace.namespace, request.contentType) { outputStream ->
            try {
                copy(request.inputStream, outputStream)
            } catch (e: Exception) {
                throw RuntimeException("Exception while uploading file", e)
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

    private fun lookupNamespace(user: User): DatasetNamespace {
        // TODO - handle failure
        val customer = registry.getCustomerByIdAsync(user.customerId!!).get()
        return DatasetNamespace(customer.namespace)
    }

    private fun ApiDag.toCytoscape() = CytoscapeDag(toCytoscapeNodes(), toCytoscapeEdges())

    private fun ApiDag.toCytoscapeNodes() = nodes.map {
        CytoscapeNode(
            CytoscapeNodeData(
                id = it.fullyQualifiedName,
                title = it.fullyQualifiedName,
                type = if (it.sources.isEmpty()) "raw" else "derived"
            )
        )
    }.toSet()

    private fun ApiDag.toCytoscapeEdges(): Set<CytoscapeEdge> {
        var i = 0L
        return nodes.flatMap { node ->
            node.sources.map { source ->
                CytoscapeEdge(
                    CytoscapeEdgeData(
                        id = i++,
                        source = nodes[source].fullyQualifiedName,
                        target = node.fullyQualifiedName
                    )
                )
            }
        }.toSet()
    }

    private val ApiDag.Node.fullyQualifiedName get() = "${namespace ?: ""}::${datasetId}"

    companion object {
        private val DEFAULT_MIME_TYPE = "application/octet-stream"
    }
}
