package io.quartic.eval.pruner

import com.fasterxml.jackson.module.kotlin.convertValue
import com.nhaarman.mockito_kotlin.*
import io.quartic.catalogue.api.CatalogueClient
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.test.assertThrows
import io.quartic.common.test.exceptionalFuture
import io.quartic.eval.EvaluatorException
import io.quartic.eval.database.model.LegacyPhaseCompleted.V1.Dataset
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Node.Raw
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Node.Step
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Source.Bucket
import io.quartic.eval.pruner.Pruner.Companion.ETAG_FIELD
import io.quartic.eval.pruner.Pruner.Etag
import io.quartic.howl.api.HowlClient
import io.quartic.howl.api.model.StorageMetadata
import io.quartic.registry.api.model.Customer
import kotlinx.coroutines.experimental.runBlocking
import okhttp3.ResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture.completedFuture

class PrunerShould {
    private val namespace = "quartic"
    private val datasetId = "yeah"
    private val key = "abc/def"

    private val customer = mock<Customer> {
        on { namespace } doReturn "default"
    }

    private val node = mock<Raw> {
        on { output } doReturn Dataset(namespace, datasetId)
        on { source } doReturn Bucket(key)
    }

    private val newInstant = Instant.now()
    private val oldInstant = newInstant - Duration.ofSeconds(100)

    private val dataset = mock<DatasetConfig> {
        on { extensions } doReturn mapOf(
            ETAG_FIELD to OBJECT_MAPPER.convertValue<Map<String, Any>>(Etag(key, oldInstant))
        )
    }

    private val metadata = mock<StorageMetadata> {
        on { lastModified } doReturn oldInstant
    }

    private val catalogue = mock<CatalogueClient> {
        on { getDatasetAsync(DatasetNamespace(namespace), DatasetId(datasetId)) } doReturn completedFuture(dataset)
    }

    private val howl = mock<HowlClient> {
        on { getUnmanagedMetadataAsync(namespace, key) } doReturn completedFuture(metadata)
    }

    private val pruner = Pruner(catalogue, howl)


    @Test
    fun prune_if_nothing_has_changed() = runBlocking {
        assertFalse(invokePruner())
    }

    @Test
    fun not_prune_if_data_has_been_modified() = runBlocking {
        whenever(metadata.lastModified).doReturn(newInstant)

        assertTrue(invokePruner())
    }

    @Test
    fun not_prune_if_different_key_is_specified() = runBlocking {
        val newKey = "ghi/jkl"
        whenever(node.source).doReturn(Bucket(newKey))
        whenever(howl.getUnmanagedMetadataAsync(namespace, newKey)).doReturn(completedFuture(metadata))

        assertTrue(invokePruner())
    }

    // The intent is that we just let Q-P handle this case
    @Test
    fun not_prune_if_data_not_found_in_howl() = runBlocking {
        whenever(howl.getUnmanagedMetadataAsync(any(), any())).doReturn(exceptionalFuture(httpException(404)))

        assertTrue(invokePruner())
    }

    @Test
    fun not_prune_if_etag_unparsable() = runBlocking {
        whenever(dataset.extensions).doReturn(mapOf(ETAG_FIELD to "gibberish"))

        assertTrue(invokePruner())
    }

    @Test
    fun not_prune_if_no_etag() = runBlocking {
        whenever(dataset.extensions).doReturn(emptyMap())

        assertTrue(invokePruner())
    }

    @Test
    fun not_prune_if_dataset_not_found_in_catalogue() = runBlocking {
        whenever(catalogue.getDatasetAsync(any(), any())).doReturn(exceptionalFuture(httpException(404)))

        assertTrue(invokePruner())
    }

    @Test
    fun not_prune_if_not_raw_node() = runBlocking {
        assertTrue(pruner.acceptorFor(customer, mock())(mock<Step>()))
    }

    @Test
    fun fallback_to_default_customer_namespace() = runBlocking {
        whenever(node.output).thenReturn(Dataset(null, datasetId))
        whenever(catalogue.getDatasetAsync(any(), any())).thenReturn(exceptionalFuture(httpException(404)))  // To prevent exception due to different namespace

        invokePruner()

        verify(catalogue).getDatasetAsync(DatasetNamespace("default"), DatasetId(datasetId))
        return@runBlocking
    }

    @Test
    fun throw_if_non_404_error_from_catalogue() {
        whenever(catalogue.getDatasetAsync(any(), any())).thenReturn(exceptionalFuture(httpException(403)))

        assertThrows<EvaluatorException> {
            runBlocking {
                invokePruner()
            }
        }
    }

    @Test
    fun throw_if_non_404_error_from_howl() {
        whenever(howl.getUnmanagedMetadataAsync(any(), any())).thenReturn(exceptionalFuture(httpException(403)))

        assertThrows<EvaluatorException> {
            runBlocking {
                invokePruner()
            }
        }
    }


    private suspend fun invokePruner() = pruner.acceptorFor(customer, mock())(node)

    private fun httpException(code: Int) = HttpException(Response.error<Any>(code, ResponseBody.create(null, "Bad")))
}
