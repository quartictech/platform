package io.quartic.eval.pruner

import com.fasterxml.jackson.module.kotlin.convertValue
import com.google.common.base.Throwables.getRootCause
import io.quartic.catalogue.api.CatalogueClient
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.common.coroutines.cancellable
import io.quartic.common.logging.logger
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.eval.Dag
import io.quartic.eval.EvaluatorException
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Node
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Node.Raw
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Node.Step
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Source.Bucket
import io.quartic.howl.api.HowlClient
import io.quartic.registry.api.model.Customer
import kotlinx.coroutines.experimental.future.await
import retrofit2.HttpException
import java.time.Instant
import java.util.concurrent.CompletableFuture

class Pruner(
    private val catalogue: CatalogueClient,
    private val howl: HowlClient
) {
    private val LOG by logger()

    fun acceptorFor(customer: Customer, dag: Dag): suspend (Node) -> Boolean {
        return { node ->
            when (node) {
                is Step -> true
                is Raw -> {
                    when (node.source) {
                        is Bucket -> isBucketObjectOutOfDate(customer, node)
                        else -> true
                    }
                }
            }
        }
    }

    // TODO - only applies to buckets
    data class Etag(
        val key: String,
        val lastModified: Instant
    )

    // TODO - going to need to do namespace authorization here
    private suspend fun isBucketObjectOutOfDate(customer: Customer, raw: Raw): Boolean {
        val namespace = raw.output.namespace ?: customer.namespace

        val oldEtag = getOldEtag(namespace, raw.output.datasetId) ?: return true
        val newEtag = calculateNewEtag(namespace, raw.source as Bucket) ?: return true
        return (newEtag != oldEtag)
    }

    private suspend fun getOldEtag(namespace: String, datasetId: String): Etag? {
        val dataset = catalogue.getDatasetAsync(
            namespace = DatasetNamespace(namespace),
            id = DatasetId(datasetId)
        ).awaitOrNullOrThrow("Catalogue")

        val raw = dataset?.extensions?.get(ETAG_FIELD)
        return if (raw != null) {
            try {
                OBJECT_MAPPER.convertValue<Etag>(raw)
            } catch (e: Exception) {
                LOG.warn("Could not parse etag", getRootCause(e))
                null
            }
        } else {
            null
        }
    }

    private suspend fun calculateNewEtag(namespace: String, bucket: Bucket): Etag? {
        // TODO - take account of bucket.name?
        val metadata = howl.getUnmanagedMetadataAsync(namespace, bucket.key)
            .awaitOrNullOrThrow("Howl")

        return if (metadata != null) {
            Etag(bucket.key, metadata.lastModified)
        } else {
            null     // Just let runner fail!
        }
    }

    /**
     * Either the result, or null in the case of 404, else throw.
     */
    private suspend fun <T> CompletableFuture<T>.awaitOrNullOrThrow(service: String): T? {
        return cancellable<T?>(
            block = { await() },
            onThrow = { t ->
                if (t is HttpException && t.code() == 404) {
                    null
                } else {
                    LOG.error("Error while communicating with ${service}", getRootCause(t))
                    throw EvaluatorException("Error while communicating with ${service}", t)
                }
            }
        )
    }

    companion object {
        val ETAG_FIELD = "etag"
    }
}
