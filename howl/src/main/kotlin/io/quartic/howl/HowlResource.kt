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
                uploadObjectOrThrow(Managed(identityNamespace, howlStorageId.uid), request)
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
            fun uploadObject(@Context request: HttpServletRequest) = uploadObjectOrThrow(coords, request)
        }

        private fun uploadObjectOrThrow(coords: StorageCoords, request: HttpServletRequest) {
            storage.putObject(
                coords,
                request.contentLength, // TODO: what if this is bigger than MAX_VALUE?
                request.contentType,
                request.inputStream
            )
        }
    }

    private fun metadataHeaders(metadata: StorageMetadata, responseBuilder: Response.ResponseBuilder) =
        responseBuilder
            .header(CONTENT_TYPE, metadata.contentType)
            .header(LAST_MODIFIED,
                DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC).format(metadata.lastModified))
            .header(CONTENT_LENGTH, metadata.contentLength)
}
