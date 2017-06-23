package io.quartic.howl

import io.quartic.common.uid.UidGenerator
import io.quartic.common.uid.randomGenerator
import io.quartic.howl.api.HowlStorageId
import io.quartic.howl.storage.Storage
import io.quartic.howl.storage.StorageCoords
import org.apache.commons.io.IOUtils
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders.CONTENT_TYPE
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

// TODO - get rid of 2D variants, and move {identity-namespace} into Resource path
@Path("/{target-namespace}")
class HowlResource(
        private val storage: Storage,
        private val howlStorageIdGenerator: UidGenerator<HowlStorageId> = randomGenerator { HowlStorageId(it) }
) {
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    fun uploadAnonymousFile(
            @PathParam("target-namespace") targetNamespace: String,
            @Context request: HttpServletRequest
    ) = uploadAnonymousFile(targetNamespace, targetNamespace, request)

    @POST
    @Path("/{identity-namespace}")
    @Produces(MediaType.APPLICATION_JSON)
    fun uploadAnonymousFile(
            @PathParam("target-namespace") targetNamespace: String,
            @PathParam("identity-namespace") identityNamespace: String,
            @Context request: HttpServletRequest
    ): HowlStorageId {
        val howlStorageId = howlStorageIdGenerator.get()
        uploadFileOrThrow(targetNamespace, identityNamespace, howlStorageId.uid, request)
        return howlStorageId
    }

    @PUT
    @Path("/{filename}")
    fun uploadFile(
            @PathParam("target-namespace") targetNamespace: String,
            @PathParam("filename") fileName: String,
            @Context request: HttpServletRequest
    ) = uploadFile(targetNamespace, targetNamespace, fileName, request)

    @PUT
    @Path("/{identity-namespace}/{filename}")
    fun uploadFile(
            @PathParam("target-namespace") targetNamespace: String,
            @PathParam("identity-namespace") identityNamespace: String,
            @PathParam("filename") fileName: String,
            @Context request: HttpServletRequest
    ) {
        uploadFileOrThrow(targetNamespace, identityNamespace, fileName, request)
    }

    private fun uploadFileOrThrow(
            targetNamespace: String,
            identityNamespace: String,
            fileName: String,
            request: HttpServletRequest
    ) = storage.putData(
            StorageCoords(targetNamespace, identityNamespace, fileName),
            request.contentType,
            request.inputStream
    ) ?: throw NotFoundException()

    @GET
    @Path("/{filename}")
    fun downloadFile(
            @PathParam("target-namespace") targetNamespace: String,
            @PathParam("filename") fileName: String
    ) = downloadFile(targetNamespace, targetNamespace, fileName)

    @GET
    @Path("/{identity-namespace}/{filename}")
    fun downloadFile(
            @PathParam("target-namespace") targetNamespace: String,
            @PathParam("identity-namespace") identityNamespace: String,
            @PathParam("filename") fileName: String
    ): Response {
        val coords = StorageCoords(targetNamespace, identityNamespace, fileName)
        val (contentType, inputStream) = storage.getData(coords, null) ?: throw NotFoundException()  // TODO: provide a useful message
        return Response.ok()
                .header(CONTENT_TYPE, contentType)
                .entity(StreamingOutput {
                    inputStream.use { inputStream -> IOUtils.copy(inputStream, it) }
                })
                .build()
    }

}
