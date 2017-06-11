package io.quartic.howl

import io.quartic.common.uid.randomGenerator
import io.quartic.howl.api.HowlStorageId
import io.quartic.howl.storage.StorageBackend
import org.apache.commons.io.IOUtils
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders.CONTENT_TYPE
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

@Path("/{namespace}")
class HowlResource(private val storageBackend: StorageBackend) {
    private val howlStorageIdGenerator = randomGenerator { HowlStorageId(it) }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    fun uploadFile(@PathParam("namespace") namespace: String,
                   @Context request: HttpServletRequest): HowlStorageId {
        val howlStorageId = howlStorageIdGenerator.get()
        storageBackend.putData(request.contentType, namespace, howlStorageId.uid, request.inputStream)
        return howlStorageId
    }

    @PUT
    @Path("/{fileName}")
    fun uploadFile(@PathParam("namespace") namespace: String,
                   @PathParam("fileName") fileName: String,
                   @Context request: HttpServletRequest) {
        storageBackend.putData(request.contentType, namespace, fileName, request.inputStream)
    }

    private fun handleDownload(namespace: String, fileName: String, version: Long?): Response {
        val (contentType, inputStream) = storageBackend.getData(namespace, fileName, version)
                ?: throw NotFoundException()  // TODO: provide a useful message

        return Response.ok()
                .header(CONTENT_TYPE, contentType)
                .entity(StreamingOutput {
                    inputStream.use { inputStream -> IOUtils.copy(inputStream, it) }
                })
                .build()
    }

    @GET
    @Path("/{fileName}")
    fun downloadFile(@PathParam("namespace") namespace: String,
                     @PathParam("fileName") fileName: String) = handleDownload(namespace, fileName, null)

    @GET
    @Path("/{fileName}/{version}")
    fun downloadFile(@PathParam("namespace") namespace: String,
                     @PathParam("fileName") fileName: String,
                     @PathParam("version") version: Long?) = handleDownload(namespace, fileName, version)
}
