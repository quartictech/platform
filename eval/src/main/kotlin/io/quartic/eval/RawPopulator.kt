package io.quartic.eval

import com.fasterxml.jackson.module.kotlin.convertValue
import com.google.common.base.Throwables.getRootCause
import io.quartic.catalogue.api.CatalogueClient
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.api.model.DatasetLocator.CloudDatasetLocator
import io.quartic.catalogue.api.model.DatasetMetadata
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.common.coroutines.cancellable
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Source.Bucket
import io.quartic.eval.database.model.PhaseCompletedV7.Node
import io.quartic.howl.api.HowlClient
import io.quartic.howl.api.model.StorageMetadata
import io.quartic.registry.api.model.Customer
import kotlinx.coroutines.experimental.future.await
import retrofit2.HttpException
import java.util.concurrent.CompletableFuture
import javax.ws.rs.core.Response.Status.NOT_FOUND
import javax.ws.rs.core.Response.Status.PRECONDITION_FAILED

class RawPopulator(
    private val catalogue: CatalogueClient,
    private val howl: HowlClient
) {
    private val LOG by logger()

    // TODO - going to need to do namespace authorization here
    // TODO - take account of bucket.name?

    suspend fun populate(customer: Customer, rawNode: Node.Raw): Boolean {
        val namespace = rawNode.output.namespace ?: customer.namespace
        val datasetId = rawNode.output.datasetId
        val destKey = (rawNode.source as Bucket).key    // TODO - what about other types?

        val oldMetadata = getOldMetadata(namespace, datasetId)
        val newMetadata = copyFromUnmanaged(namespace, datasetId, destKey, oldMetadata?.eTag)

        updateCatalogue(
            namespace,
            datasetId,
            rawNode,
            newMetadata ?: oldMetadata!!    // They can't both be null!
        )

        return (newMetadata != null)
    }

    private suspend fun getOldMetadata(namespace: String, datasetId: String): StorageMetadata? {
        val dataset = catalogue.getDatasetAsync(
            namespace = DatasetNamespace(namespace),
            id = DatasetId(datasetId)
        ).awaitOrThrowOnError("Catalogue")

        val raw = dataset?.extensions?.get(HOWL_METADATA_FIELD)
        return if (raw != null) {
            try {
                OBJECT_MAPPER.convertValue<StorageMetadata>(raw)
            } catch (e: Exception) {
                LOG.warn("Could not parse metadata", getRootCause(e))
                null
            }
        } else {
            null
        }
    }

    private suspend fun copyFromUnmanaged(namespace: String, destKey: String, sourceKey: String, oldETag: String?) =
        howl.copyObjectFromUnmanaged(
            targetNamespace = namespace,
            identityNamespace = namespace,
            destKey = destKey,
            sourceKey = sourceKey,
            oldETag = oldETag
        ).awaitOrThrowOnError("Howl", PRECONDITION_FAILED.statusCode)

    private fun getDescription(node: Node.Raw): String =
        node.metadata[DESCRIPTION].let { maybeDescription ->
            when (maybeDescription) {
                is String -> maybeDescription
                else -> node.name
            }
        }

    private suspend fun updateCatalogue(
        namespace: String,
        datasetId: String,
        rawNode: Node.Raw,
        metadata: StorageMetadata
    ) =
        catalogue.registerOrUpdateDatasetAsync(
            DatasetNamespace(namespace),
            DatasetId(datasetId),
            DatasetConfig(
                DatasetMetadata(
                    name = rawNode.name,
                    description = getDescription(rawNode),
                    attribution = "quartic" // TODO - this is silly
                ),
                CloudDatasetLocator(
                    path = HowlClient.locatorPath(namespace, datasetId),
                    mimeType = metadata.contentType
                ),
                mapOf(HOWL_METADATA_FIELD to metadata)
            )
        ).awaitOrThrowOnError("Catalogue")

    /**
     * Either the result, or null in the case of 404, else throw.
     */
    private suspend fun <T> CompletableFuture<T>.awaitOrThrowOnError(
        service: String,
        allowableErrorCode: Int = NOT_FOUND.statusCode
    ): T? {
        return cancellable<T?>(
            block = { await() },
            onThrow = { t ->
                if (t is HttpException && t.code() == allowableErrorCode) {
                    null
                } else {
                    LOG.error("Error while communicating with ${service}", getRootCause(t))
                    throw EvaluatorException("Error while communicating with ${service}", t)
                }
            }
        )
    }

    companion object {
        val HOWL_METADATA_FIELD = "howl"
        val DESCRIPTION = "description"
    }
}
