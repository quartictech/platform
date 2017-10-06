package io.quartic.howl

import io.quartic.common.uid.UidGenerator
import io.quartic.common.uid.randomGenerator
import io.quartic.howl.api.HowlClient.Companion.UNMANAGED_SOURCE_KEY_HEADER
import io.quartic.howl.api.model.HowlStorageId
import io.quartic.howl.api.model.StorageMetadata
import io.quartic.howl.storage.StorageCoords
import io.quartic.howl.storage.StorageCoords.Managed
import io.quartic.howl.storage.StorageCoords.Unmanaged
import io.quartic.howl.storage.StorageFactory
import org.apache.commons.io.IOUtils
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR
import javax.ws.rs.core.Response.Status.PRECONDITION_FAILED
import javax.ws.rs.core.StreamingOutput

@Path("/")
class HowlResource(
    private val storageFactory: StorageFactory,
    private val howlStorageIdGenerator: UidGenerator<HowlStorageId> = randomGenerator { HowlStorageId(it) }
) {
    @Path("/{target-namespace}")
    fun namespaceResource(@PathParam("target-namespace") targetNamespace: String) = object : Any() {
        private val storage = storageFactory.createFor(targetNamespace)
            ?: throw NotFoundException("Cannot find namespace '${targetNamespace}'")

        @Path("/unmanaged/{key}")
        fun unmanagedResource(@PathParam("key") key: String) = ReadableObjectResource(Unmanaged(key))

        @Path("/managed/{identity-namespace}")
        fun managedResource(@PathParam("identity-namespace") identityNamespace: String) = object : Any() {

            @Path("/{key}")
            fun writableResource(@PathParam("key") key: String) = WritableObjectResource(Managed(identityNamespace, key))

            // TODO - eliminate this endpoint
            @POST
            @Produces(MediaType.APPLICATION_JSON)
            fun uploadAnonymousObject(@Context request: HttpServletRequest): HowlStorageId {
                val howlStorageId = howlStorageIdGenerator.get()
                uploadObject(request, Managed(identityNamespace, howlStorageId.uid))
                return howlStorageId
            }
        }

        private open inner class ReadableObjectResource(protected val coords: StorageCoords) {
            @GET
            fun downloadObject(): Response {
                val (metadata, inputStream) = storage.getObject(coords) ?: throw NotFoundException()  // TODO: provide a useful message
                return metadataHeaders(metadata, Response.ok())
                    .entity(StreamingOutput { inputStream.use { istream -> IOUtils.copy(istream, it) } })
                    .build()
            }

            @HEAD
            fun getMetadata(): Response {
                val metadata = storage.getMetadata(coords) ?: throw NotFoundException()  // TODO: provide a useful message
                return metadataHeaders(metadata, Response.ok()).build()
            }
        }

        private inner class WritableObjectResource(coords: StorageCoords) : ReadableObjectResource(coords) {
            @PUT
            @Produces(MediaType.APPLICATION_JSON)
            fun uploadOrCopyObject(
                @HeaderParam(UNMANAGED_SOURCE_KEY_HEADER) unmanagedSourceKey: String?,
                @HeaderParam(IF_NONE_MATCH) oldETag: String?,
                @Context request: HttpServletRequest
            ): Any = when {
                unmanagedSourceKey != null -> copyObject(Unmanaged(unmanagedSourceKey), coords, oldETag)
                else -> uploadObject(request, coords)
            }
        }

        private fun copyObject(source: StorageCoords, dest: StorageCoords, oldETag: String?): StorageMetadata {
            val newMetadata = doOr500 { storage.copyObject(source, dest, oldETag) }
            return when (newMetadata?.eTag) {
                null -> throw NotFoundException()  // TODO: provide a useful message
                oldETag -> throw WebApplicationException(PRECONDITION_FAILED)   // See https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.26
                else -> newMetadata
            }
        }

        private fun uploadObject(request: HttpServletRequest, dest: StorageCoords) = doOr500 {
            storage.putObject(
                request.contentLength,
                request.contentType, // TODO: what if this is bigger than MAX_VALUE?
                request.inputStream,
                dest
            )
        }
    }

    private fun <T> doOr500(block: () -> T) =
        try { block() }
        catch (e: Exception) { throw ServerErrorException(INTERNAL_SERVER_ERROR) }

    private fun metadataHeaders(metadata: StorageMetadata, responseBuilder: Response.ResponseBuilder) =
        responseBuilder
            .header(CONTENT_TYPE, metadata.contentType)
            .header(CONTENT_LENGTH, metadata.contentLength)
            .header(ETAG, metadata.eTag)
}
