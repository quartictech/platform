package io.quartic.howl

import io.quartic.common.uid.randomGenerator
import io.quartic.howl.api.HowlStorageId
import io.quartic.howl.storage.StorageBackend
import io.quartic.howl.storage.StorageCoords
import org.apache.commons.io.IOUtils
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders.CONTENT_TYPE
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

@Path("/{target-namespace}")
class HowlResource(private val storageBackend: StorageBackend) {
    private val howlStorageIdGenerator = randomGenerator { HowlStorageId(it) }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    fun uploadFile(@PathParam("target-namespace") targetNamespace: String,
                   @Context request: HttpServletRequest): HowlStorageId {
        val howlStorageId = howlStorageIdGenerator.get()
        storageBackend.putData(
                StorageCoords(targetNamespace, targetNamespace, howlStorageId.uid),
                request.contentType,
                request.inputStream
        )
        return howlStorageId
    }

    @PUT
    @Path("/{fileName}")
    fun uploadFile(@PathParam("target-namespace") targetNamespace: String,
                   @PathParam("fileName") fileName: String,
                   @Context request: HttpServletRequest) {
        storageBackend.putData(
                StorageCoords(targetNamespace, targetNamespace, fileName),
                request.contentType,
                request.inputStream
        )
    }

    private fun handleDownload(targetNamespace: String, fileName: String, version: Long?): Response {
        val (contentType, inputStream) = storageBackend.getData(
                StorageCoords(targetNamespace, targetNamespace, fileName), version
        ) ?: throw NotFoundException()  // TODO: provide a useful message

        return Response.ok()
                .header(CONTENT_TYPE, contentType)
                .entity(StreamingOutput {
                    inputStream.use { inputStream -> IOUtils.copy(inputStream, it) }
                })
                .build()
    }

    @GET
    @Path("/{fileName}")
    fun downloadFile(@PathParam("target-namespace") targetNamespace: String,
                     @PathParam("fileName") fileName: String) = handleDownload(targetNamespace, fileName, null)

    @GET
    @Path("/{fileName}/{version}")
    fun downloadFile(@PathParam("target-namespace") targetNamespace: String,
                     @PathParam("fileName") fileName: String,
                     @PathParam("version") version: Long?) = handleDownload(targetNamespace, fileName, version)
}
