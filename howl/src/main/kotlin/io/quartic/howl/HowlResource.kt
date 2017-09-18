package io.quartic.howl

import io.quartic.common.uid.UidGenerator
import io.quartic.common.uid.randomGenerator
import io.quartic.howl.api.HowlStorageId
import io.quartic.howl.storage.Storage
import io.quartic.howl.storage.StorageCoords
import io.quartic.howl.storage.StorageCoords.Managed
import io.quartic.howl.storage.StorageCoords.Unmanaged
import org.apache.commons.io.IOUtils
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.*
import javax.ws.rs.core.HttpHeaders.CONTENT_TYPE
import javax.ws.rs.core.HttpHeaders.LAST_MODIFIED
import javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH

@Path("/{target-namespace}")
class HowlResource(
    private val storage: Storage,
    private val howlStorageIdGenerator: UidGenerator<HowlStorageId> = randomGenerator { HowlStorageId(it) }
) {
    @GET
    @Path("/unmanaged/{key}")
    fun downloadUnmanaged(
        @PathParam("target-namespace") targetNamespace: String,
        @PathParam("key") key: String
    ) = downloadFile(Unmanaged(targetNamespace, key))

    @HEAD
    @Path("/unmanaged/{key}")
    fun headUnmanaged(
        @PathParam("target-namespace") targetNamespace: String,
        @PathParam("key") key: String
    ) = headFile(Unmanaged(targetNamespace, key))

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
        fun uploadAnonymousFile(@Context request: HttpServletRequest): HowlStorageId {
            val howlStorageId = howlStorageIdGenerator.get()
            uploadFileOrThrow(identityNamespace, howlStorageId.uid, request)
            return howlStorageId
        }

        @PUT
        @Path("/{key}")
        fun uploadFile(@PathParam("key") key: String, @Context request: HttpServletRequest) {
            uploadFileOrThrow(identityNamespace, key, request)
        }

        @GET
        @Path("/{key}")
        fun downloadFile(@PathParam("key") key: String) = downloadFile(Managed(targetNamespace, identityNamespace, key))

        @HEAD
        @Path("/{key}")
        fun headFile(@PathParam("key") key: String) = headFile(Managed(targetNamespace, identityNamespace, key))

        private fun uploadFileOrThrow(
            identityNamespace: String,
            key: String,
            request: HttpServletRequest
        ) = storage.putData(
            Managed(targetNamespace, identityNamespace, key),
            request.contentLength, // TODO: what if this is bigger than MAX_VALUE?
            request.contentType,
            request.inputStream
        ) ?: throw NotFoundException("Storage backend could not write file")
    }


    private fun downloadFile(coords: StorageCoords): Response {
        val (metadata, inputStream) = storage.getData(coords, null) ?: throw NotFoundException()  // TODO: provide a useful message
        return Response.ok()
            .header(CONTENT_TYPE, metadata.contentType)
            .header(LAST_MODIFIED, DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC)
                .format(metadata.lastModified))
            .header(CONTENT_LENGTH, metadata.contentLength)
            .entity(StreamingOutput {
                inputStream.use { istream -> IOUtils.copy(istream, it) }
            })
            .build()
    }

    private fun headFile(coords: StorageCoords): Response {
        val metadata = storage.getMetadata(coords, null) ?: throw NotFoundException()  // TODO: provide a useful message
        return Response.ok()
            .header(CONTENT_TYPE, metadata.contentType)
            .header(LAST_MODIFIED, DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC)
                .format(metadata.lastModified))
            .header(CONTENT_LENGTH, metadata.contentLength)
            .build()
    }

}
