package io.quartic.mgmt

import io.quartic.catalogue.api.CatalogueService
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.api.model.DatasetLocator
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.common.geojson.GeoJsonParser
import io.quartic.howl.api.HowlService
import io.quartic.howl.api.HowlStorageId
import io.quartic.mgmt.conversion.CsvConverter
import org.apache.commons.io.IOUtils
import java.io.IOException
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@Path("/")
class MgmtResource(
        private val catalogue: CatalogueService,
        private val howl: HowlService,
        private val defaultCatalogueNamespace: DatasetNamespace
) {
    @GET
    @Path("/dataset")
    @Produces(MediaType.APPLICATION_JSON)
    fun getDatasets(): Map<DatasetId, DatasetConfig> = catalogue.getDatasets().getOrDefault(defaultCatalogueNamespace, emptyMap())

    @DELETE
    @Path("/dataset/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun deleteDataset(@PathParam("id") id: DatasetId) {
        catalogue.deleteDataset(defaultCatalogueNamespace, id)
    }

    @POST
    @Path("/dataset")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun createDataset(request: CreateDatasetRequest): DatasetId {
        val datasetConfig = when (request) {
            is CreateStaticDatasetRequest -> {
                try {
                    val name = preprocessFile(request.fileName, request.fileType)
                    val locator = DatasetLocator.CloudDatasetLocator(
                            "/%s/%s".format(HOWL_NAMESPACE, name),
                            false,
                            request.mimeType())
                    DatasetConfig(
                            request.metadata,
                            locator,
                            emptyMap()
                    )
                } catch (e: IOException) {
                    throw RuntimeException("Exception while preprocessing file", e)
                }
            }
            else -> throw BadRequestException("Unknown request type '${request.javaClass.simpleName}'")
        }

        return catalogue.registerDataset(defaultCatalogueNamespace, datasetConfig).id
    }

    private fun preprocessFile(fileName: String, fileType: FileType): String {
        return howl
                .downloadFile(HOWL_NAMESPACE, fileName)
                .orElseThrow { NotFoundException("File not found: " + fileName) }
                .use { inputStream ->
                    when (fileType) {
                        FileType.GEOJSON -> {
                            try {
                                GeoJsonParser(inputStream).validate()
                            } catch (e: Exception) {
                                throw BadRequestException("Exception while validating GeoJSON", e)
                            }
                            fileName
                        }
                        FileType.CSV -> {
                            val storageId = howl.uploadFile(MediaType.APPLICATION_JSON, HOWL_NAMESPACE) { outputStream ->
                                try {
                                    CsvConverter().convert(inputStream, outputStream)
                                } catch (e: IOException) {
                                    throw BadRequestException("Exception while converting CSV to GeoJSON", e)
                                }
                            }
                            storageId.uid
                        }
                        FileType.RAW -> fileName
                        else -> fileName
                    }
                }
    }

    @POST
    @Path("/file")
    @Produces(MediaType.APPLICATION_JSON)
    fun uploadFile(@Context request: HttpServletRequest): HowlStorageId {
        return howl.uploadFile(request.contentType, HOWL_NAMESPACE) { outputStream ->
            try {
                IOUtils.copy(request.inputStream, outputStream)
            } catch (e: Exception) {
                throw RuntimeException("Exception while uploading file: " + e)
            }
        }
    }

    companion object {
        private val HOWL_NAMESPACE = "mgmt"
    }
}
