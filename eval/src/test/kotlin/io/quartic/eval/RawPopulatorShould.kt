package io.quartic.eval

import com.fasterxml.jackson.module.kotlin.convertValue
import com.nhaarman.mockito_kotlin.*
import io.quartic.catalogue.api.CatalogueClient
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.api.model.DatasetLocator.CloudDatasetLocator
import io.quartic.catalogue.api.model.DatasetMetadata
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.common.test.assertThrows
import io.quartic.common.test.exceptionalFuture
import io.quartic.eval.RawPopulator.Companion.HOWL_METADATA_FIELD
import io.quartic.eval.database.model.LegacyPhaseCompleted.V1.Dataset
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.LexicalInfo
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Node.Raw
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Source.Bucket
import io.quartic.howl.api.HowlClient
import io.quartic.howl.api.HowlClient.Companion.locatorPath
import io.quartic.howl.api.model.StorageMetadata
import io.quartic.registry.api.model.Customer
import kotlinx.coroutines.experimental.runBlocking
import okhttp3.ResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.util.concurrent.CompletableFuture.completedFuture
import javax.ws.rs.core.Response.Status.PRECONDITION_FAILED

class RawPopulatorShould {
    private val namespace = "quartic"
    private val datasetId = "yeah"
    private val key = "abc/def"
    private val oldMetadata = StorageMetadata("application/noob", 42, "old-etag")
    private val newMetadata = StorageMetadata("application/leet", 69, "new-etag")

    private val customer = mock<Customer> {
        on { namespace } doReturn "default"
    }

    private val node = mock<Raw> {
        on { info } doReturn LexicalInfo(
            name = "foo",
            description = "bar",
            file = "whatever",
            lineRange = emptyList()
        )
        on { output } doReturn Dataset(namespace, datasetId)
        on { source } doReturn Bucket(key)
    }

    private val dataset = mock<DatasetConfig> {
        on { extensions } doReturn mapOf(
            HOWL_METADATA_FIELD to OBJECT_MAPPER.convertValue<Map<String, Any>>(oldMetadata)
        )
    }

    private val catalogue = mock<CatalogueClient> {
        on { getDatasetAsync(DatasetNamespace(namespace), DatasetId(datasetId)) } doReturn completedFuture(dataset)
        on { registerOrUpdateDatasetAsync(any(), any(), any()) } doReturn completedFuture(mock())
    }

    private val howl = mock<HowlClient> {
        on { copyObjectFromUnmanaged(any(), any(), any(), any(), anyOrNull()) } doReturn completedFuture(newMetadata)
    }

    private val populator = RawPopulator(catalogue, howl)

    @Test
    fun update_catalogue_with_old_metadata_if_copy_skipped() {
        whenever(howl.copyObjectFromUnmanaged(namespace, namespace, datasetId, key, oldMetadata.eTag))
            .thenReturn(exceptionalFuture(httpException(PRECONDITION_FAILED.statusCode)))

        val result = invokePopulator()

        assertFalse(result)
        verify(catalogue).registerOrUpdateDatasetAsync(
            DatasetNamespace(namespace),
            DatasetId(datasetId),
            DatasetConfig(
                DatasetMetadata("foo", "bar", "quartic"),
                CloudDatasetLocator(locatorPath(namespace, datasetId), false, oldMetadata.contentType),
                mapOf(HOWL_METADATA_FIELD to oldMetadata)
            )
        )
    }

    @Test
    fun update_catalogue_with_new_metadata_if_copy_skipped() {
        val result = invokePopulator()

        assertTrue(result)
        verify(catalogue).registerOrUpdateDatasetAsync(
            DatasetNamespace(namespace),
            DatasetId(datasetId),
            DatasetConfig(
                DatasetMetadata("foo", "bar", "quartic"),
                CloudDatasetLocator(locatorPath(namespace, datasetId), false, newMetadata.contentType),
                mapOf(HOWL_METADATA_FIELD to newMetadata)
            )
        )
    }

    @Test
    fun use_null_etag_if_extension_unparsable() {
        whenever(dataset.extensions).thenReturn(mapOf(HOWL_METADATA_FIELD to "gibberish"))

        invokePopulator()

        verify(howl).copyObjectFromUnmanaged(any(), any(), any(), any(), eq(null))
    }

    @Test
    fun use_null_etag_if_extension_missing() {
        whenever(dataset.extensions).thenReturn(emptyMap())

        invokePopulator()

        verify(howl).copyObjectFromUnmanaged(any(), any(), any(), any(), eq(null))
    }

    @Test
    fun use_null_etag_if_dataset_missing() {
        whenever(catalogue.getDatasetAsync(any(), any())).doReturn(exceptionalFuture(httpException(404)))

        invokePopulator()

        verify(howl).copyObjectFromUnmanaged(any(), any(), any(), any(), eq(null))
    }

    @Test
    fun fallback_to_default_customer_namespace() {
        whenever(node.output).thenReturn(Dataset(null, datasetId))
        whenever(catalogue.getDatasetAsync(any(), any())).thenReturn(exceptionalFuture(httpException(404)))  // To prevent exception due to different namespace

        invokePopulator()

        verify(catalogue).getDatasetAsync(eq(DatasetNamespace("default")), any())
    }

    @Test
    fun throw_if_non_404_error_from_catalogue() {
        whenever(catalogue.getDatasetAsync(any(), any())).thenReturn(exceptionalFuture(httpException(403)))

        assertThrows<EvaluatorException> { invokePopulator() }
    }

    @Test
    fun throw_if_non_412_error_from_howl() {
        whenever(howl.copyObjectFromUnmanaged(any(), any(), any(), any(), anyOrNull())).thenReturn(exceptionalFuture(httpException(404)))

        assertThrows<EvaluatorException> { invokePopulator() }
    }

    private fun invokePopulator() = runBlocking { populator.populate(customer, node) }

    private fun httpException(code: Int) = HttpException(Response.error<Any>(code, ResponseBody.create(null, "Bad")))
}
