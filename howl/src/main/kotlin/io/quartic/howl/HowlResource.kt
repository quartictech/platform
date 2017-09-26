package io.quartic.howl

import io.quartic.common.uid.UidGenerator
import io.quartic.common.uid.randomGenerator
import io.quartic.howl.api.model.HowlStorageId
import io.quartic.howl.api.model.StorageMetadata
import io.quartic.howl.storage.Storage
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

@Path("/{target-namespace}")
class HowlResource(
    private val storage: Storage,
    private val howlStorageIdGenerator: UidGenerator<HowlStorageId> = randomGenerator { HowlStorageId(it) }
) {
    @GET
    @Path("/unmanaged/{key}")
    fun downloadUnmanagedObject(
        @PathParam("target-namespace") targetNamespace: String,
        @PathParam("key") key: String
    ) = downloadObject(Unmanaged(targetNamespace, key))

    @HEAD
    @Path("/unmanaged/{key}")
    fun getUnmanagedMetadata(
        @PathParam("target-namespace") targetNamespace: String,
        @PathParam("key") key: String
    ) = getMetadata(Unmanaged(targetNamespace, key))

    @Path("/managed/{identity-namespace}")
    fun managedResource(
        @PathParam("target-namespace") targetNamespace: String,
        @PathParam("identity-namespace") identityNamespace: String
    ) = ManagedHowlResource(targetNamespace, identityNamespace)

    inner class ManagedHowlResource(
        private val targetNamespace: String,
        private val identityNamespace: String
    ) {
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
            downloadObject(Managed(targetNamespace, identityNamespace, key))

        @HEAD
        @Path("/{key}")
        fun getManagedMetadata(@PathParam("key") key: String) =
            getMetadata(Managed(targetNamespace, identityNamespace, key))

        private fun uploadObjectOrThrow(
            identityNamespace: String,
            key: String,
            request: HttpServletRequest
        ) {
            if (!storage.putObject(
                Managed(targetNamespace, identityNamespace, key),
                request.contentLength, // TODO: what if this is bigger than MAX_VALUE?
                request.contentType,
                request.inputStream
            )) {
                throw NotFoundException("Storage backend could not write file")
            }
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

    private fun metadataHeaders(metadata: StorageMetadata, responseBuilder: Response.ResponseBuilder) =
        responseBuilder
            .header(CONTENT_TYPE, metadata.contentType)
            .header(LAST_MODIFIED, DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC)
                .format(metadata.lastModified))
            .header(CONTENT_LENGTH, metadata.contentLength)



}
