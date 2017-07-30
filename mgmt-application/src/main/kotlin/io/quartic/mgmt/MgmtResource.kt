package io.quartic.mgmt

import io.dropwizard.auth.Auth
import io.quartic.catalogue.api.CatalogueService
import io.quartic.catalogue.api.model.*
import io.quartic.common.auth.User
import io.quartic.common.geojson.GeoJsonParser
import io.quartic.howl.api.HowlService
import io.quartic.howl.api.HowlStorageId
import io.quartic.mgmt.auth.NamespaceAuthoriser
import io.quartic.mgmt.conversion.CsvConverter
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.io.InputStream
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/")
class MgmtResource(
        private val catalogue: CatalogueService,
        private val howl: HowlService,
        private val authoriser: NamespaceAuthoriser
) {
    // TODO - frontend will need to cope with DatasetNamespace in request paths and GET /datasets response

    // TODO - how does frontend know what namespace to use for dataset creation?

    @GET
    @Path("/datasets")
    @Produces(MediaType.APPLICATION_JSON)
    fun getDatasets(@Auth user: User): Response {
        return Response.ok().status(401).build()
//        catalogue.getDatasets().filterKeys { namespace -> authoriser.authorisedFor(user, namespace) }
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
                    val locator = DatasetLocator.CloudDatasetLocator(
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
                FileType.GEOJSON -> {
                    try {
                        GeoJsonParser(s).validate()
                    } catch (e: Exception) {
                        throw BadRequestException("Exception while validating GeoJSON", e)
                    }
                    fileName
                }
                FileType.CSV -> {
                    val storageId = howl.uploadAnonymousFile(namespace, MediaType.APPLICATION_JSON) { outputStream ->
                        try {
                            CsvConverter().convert(s, outputStream)
                        } catch (e: IOException) {
                            throw BadRequestException("Exception while converting CSV to GeoJSON", e)
                        }
                    }
                    storageId.uid
                }
                FileType.RAW -> fileName
                else -> fileName
            }
        }
    }

    // No need for auth injection here, as there's no interaction with dataset namespaces
    @POST
    @Path("/file/{namespace}")
    @Produces(MediaType.APPLICATION_JSON)
    fun uploadFile(
            @PathParam("namespace") namespace: DatasetNamespace,
            @Context request: HttpServletRequest): HowlStorageId {
        return howl.uploadAnonymousFile(namespace.namespace, request.contentType) { outputStream ->
            try {
                IOUtils.copy(request.inputStream, outputStream)
            } catch (e: Exception) {
                throw RuntimeException("Exception while uploading file: " + e)
            }
        }
    }

    private fun notFoundException(type: String, name: String) = NotFoundException("$type '$name' not found")

    private fun throwIfDatasetNotPresentOrNotAllowed(user: User, coords: DatasetCoordinates) {
        val datasets = getDatasets(user)    // These will already be filtered to those that the user is authorised for

        val datasetsInNamespace = datasets[coords.namespace]
                ?: throw notFoundException("Namespace", coords.namespace.namespace)
        if (!datasetsInNamespace.contains(coords.id)) {
            throw notFoundException("Dataset", coords.id.uid)
        }
    }

    private fun throwIfNamespaceNotAllowed(user: User, namespace: DatasetNamespace) {
        if (!authoriser.authorisedFor(user, namespace)) {
            throw notFoundException("Namespace", namespace.namespace) // 404 instead of 403 to prevent discovery
        }
    }
}
