package io.quartic.howl

import io.quartic.common.uid.UidGenerator
import io.quartic.common.uid.randomGenerator
import io.quartic.howl.api.model.HowlStorageId
import io.quartic.howl.api.model.StorageMetadata
import io.quartic.howl.storage.StorageCoords
import io.quartic.howl.storage.StorageCoords.Managed
import io.quartic.howl.storage.StorageCoords.Unmanaged
import io.quartic.howl.storage.StorageFactory
import org.apache.commons.io.IOUtils
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
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
                @HeaderParam(IF_NONE_MATCH) oldEtag: String?,
                @Context request: HttpServletRequest
            ) = if (unmanagedSourceKey != null) {
                copyObject(Unmanaged(unmanagedSourceKey), coords, oldEtag)
            } else {
                uploadObject(request, coords)
            }
        }

        private fun copyObject(source: StorageCoords, dest: StorageCoords, oldEtag: String?): String {
            val newEtag = try {
                storage.copyObject(source, dest, oldEtag)
            } catch (e: Exception) {
                throw ServerErrorException(INTERNAL_SERVER_ERROR)
            }
            return when (newEtag) {
                null -> throw NotFoundException()  // TODO: provide a useful message
                oldEtag -> throw WebApplicationException(PRECONDITION_FAILED)   // See https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.26
                else -> newEtag
            }
        }

        private fun uploadObject(request: HttpServletRequest, dest: StorageCoords) = try {
            storage.putObject(
                request.contentLength,
                request.contentType, // TODO: what if this is bigger than MAX_VALUE?
                request.inputStream,
                dest
            )
        } catch (e: Exception) {
            throw ServerErrorException(INTERNAL_SERVER_ERROR)
        }
    }

    private fun metadataHeaders(metadata: StorageMetadata, responseBuilder: Response.ResponseBuilder) =
        responseBuilder
            .header(CONTENT_TYPE, metadata.contentType)
            .header(LAST_MODIFIED,
                DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC).format(metadata.lastModified))
            .header(CONTENT_LENGTH, metadata.contentLength)

    companion object {
        const val UNMANAGED_SOURCE_KEY_HEADER = "x-unmanaged-source-key"
    }
}
