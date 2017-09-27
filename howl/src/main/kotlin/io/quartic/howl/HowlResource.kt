package io.quartic.howl

import io.quartic.common.uid.UidGenerator
import io.quartic.common.uid.randomGenerator
import io.quartic.howl.api.model.HowlStorageId
import io.quartic.howl.api.model.StorageMetadata
import io.quartic.howl.storage.StorageCoords
import io.quartic.howl.storage.StorageCoords.Managed
import io.quartic.howl.storage.StorageCoords.Unmanaged
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

        @GET
        @Path("/unmanaged/{key}")
        fun downloadUnmanagedObject(@PathParam("key") key: String) = downloadObject(Unmanaged(key))

        @HEAD
        @Path("/unmanaged/{key}")
        @Produces(MediaType.APPLICATION_JSON)
        fun getUnmanagedMetadata(@PathParam("key") key: String) = getMetadata(Unmanaged(key))

        @Path("/managed/{identity-namespace}")
        fun managedResource(@PathParam("identity-namespace") identityNamespace: String) = object : Any() {

            @POST
            @Produces(MediaType.APPLICATION_JSON)
            fun uploadAnonymousManagedObject(@Context request: HttpServletRequest): HowlStorageId {
                val howlStorageId = howlStorageIdGenerator.get()
                uploadObjectOrThrow(identityNamespace, howlStorageId.uid, request)
                return howlStorageId
            }

            @PUT
            @Path("/{key}")
            fun uploadManagedObject(@PathParam("key") key: String, @Context request: HttpServletRequest) {
                uploadObjectOrThrow(identityNamespace, key, request)
            }

            @GET
            @Path("/{key}")
            fun downloadManagedObject(@PathParam("key") key: String) =
                downloadObject(Managed(identityNamespace, key))

            @HEAD
            @Path("/{key}")
            @Produces(MediaType.APPLICATION_JSON)
            fun getManagedMetadata(@PathParam("key") key: String) =
                getMetadata(Managed(identityNamespace, key))

            private fun uploadObjectOrThrow(
                identityNamespace: String,
                key: String,
                request: HttpServletRequest
            ) {
                storage.putObject(
                    Managed(identityNamespace, key),
                    request.contentLength, // TODO: what if this is bigger than MAX_VALUE?
                    request.contentType,
                    request.inputStream
                )
            }
        }

        private fun downloadObject(coords: StorageCoords): Response {
            val (metadata, inputStream) = storage.getObject(coords) ?: throw NotFoundException()  // TODO: provide a useful message
            return metadataHeaders(metadata, Response.ok())
                .entity(StreamingOutput {
                    inputStream.use { istream -> IOUtils.copy(istream, it) }
                })
                .build()
        }

        private fun getMetadata(coords: StorageCoords): Response {
            val metadata = storage.getMetadata(coords) ?: throw NotFoundException()  // TODO: provide a useful message
            return metadataHeaders(metadata, Response.ok())
                .build()
        }
    }

    private fun metadataHeaders(metadata: StorageMetadata, responseBuilder: Response.ResponseBuilder) =
        responseBuilder
            .header(CONTENT_TYPE, metadata.contentType)
            .header(LAST_MODIFIED,
                DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC).format(metadata.lastModified))
            .header(CONTENT_LENGTH, metadata.contentLength)



}
