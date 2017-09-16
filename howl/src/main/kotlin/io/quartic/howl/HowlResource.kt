package io.quartic.howl

import io.quartic.common.uid.UidGenerator
import io.quartic.common.uid.randomGenerator
import io.quartic.howl.api.HowlStorageId
import io.quartic.howl.storage.Storage
import io.quartic.howl.storage.StorageCoords
import io.quartic.howl.storage.StorageCoords.Managed
import io.quartic.howl.storage.StorageCoords.Unmanaged
import org.apache.commons.io.IOUtils
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders.CONTENT_TYPE
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

// TODO - get rid of 2D variants
@Path("/{target-namespace}")
class HowlResource(
    private val storage: Storage,
    private val howlStorageIdGenerator: UidGenerator<HowlStorageId> = randomGenerator { HowlStorageId(it) }
) {
    @GET
    @Path("/unmanaged/{key}")
    fun unmanagedResource(
        @PathParam("target-namespace") targetNamespace: String,
        @PathParam("key") key: String
    ) = downloadFile(Unmanaged(targetNamespace, key))

    @Path("/managed")
    fun managedResource(
        @PathParam("target-namespace") targetNamespace: String
    ) = ManagedHowlResource(targetNamespace)

    inner class ManagedHowlResource(private val targetNamespace: String) {
        @POST
        @Produces(MediaType.APPLICATION_JSON)
        fun uploadAnonymousFile(
            @Context request: HttpServletRequest
        ) = uploadAnonymousFile(targetNamespace, request)

        @POST
        @Path("/{identity-namespace}")
        @Produces(MediaType.APPLICATION_JSON)
        fun uploadAnonymousFile(
            @PathParam("identity-namespace") identityNamespace: String,
            @Context request: HttpServletRequest
        ): HowlStorageId {
            val howlStorageId = howlStorageIdGenerator.get()
            uploadFileOrThrow(identityNamespace, howlStorageId.uid, request)
            return howlStorageId
        }

        @PUT
        @Path("/{key}")
        fun uploadFile(
            @PathParam("key") key: String,
            @Context request: HttpServletRequest
        ) = uploadFile(targetNamespace, key, request)

        @PUT
        @Path("/{identity-namespace}/{key}")
        fun uploadFile(
            @PathParam("identity-namespace") identityNamespace: String,
            @PathParam("key") key: String,
            @Context request: HttpServletRequest
        ) {
            uploadFileOrThrow(identityNamespace, key, request)
        }

        @GET
        @Path("/{key}")
        fun downloadFile(
            @PathParam("key") key: String
        ) = downloadFile(targetNamespace, key)

        @GET
        @Path("/{identity-namespace}/{key}")
        fun downloadFile(
            @PathParam("identity-namespace") identityNamespace: String,
            @PathParam("key") key: String
        ) = downloadFile(Managed(targetNamespace, identityNamespace, key))

        private fun uploadFileOrThrow(
            identityNamespace: String,
            key: String,
            request: HttpServletRequest
        ) = storage.putData(
            Managed(targetNamespace, identityNamespace, key),
            request.contentLength, // TODO: what if this is bigger than MAX_VALUE?
            request.contentType,
            request.inputStream
        ) ?: throw NotFoundException()
    }

    private fun downloadFile(coords: StorageCoords): Response {
        val (contentType, inputStream) = storage.getData(coords, null) ?: throw NotFoundException()  // TODO: provide a useful message
        return Response.ok()
            .header(CONTENT_TYPE, contentType)
            .entity(StreamingOutput {
                inputStream.use { istream -> IOUtils.copy(istream, it) }
            })
            .build()
    }

}
