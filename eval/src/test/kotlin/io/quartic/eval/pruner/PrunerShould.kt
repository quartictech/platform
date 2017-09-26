package io.quartic.eval.pruner

import com.fasterxml.jackson.module.kotlin.convertValue
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.quartic.catalogue.api.CatalogueClient
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.howl.api.HowlClient
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture.completedFuture

class PrunerShould {

    private val catalogue = mock<CatalogueClient>()
    private val howl = mock<HowlClient>()
    private val pruner = Pruner(catalogue, howl)

    private val namespace = "quartic"
    private val datasetId = "yeah"
    private val key = "abc/def"

    private val dataset = mock<DatasetConfig> {
        on { extensions } doReturn mapOf(
            Pruner.ETAG_FIELD to OBJECT_MAPPER.convertValue<Map<String, Any>>(Pruner.Etag(
                key, Instant.now() - Duration.ofSeconds(100)
            ))
        )
    }

    @Test
    fun return_false_if_etags_match() {
        whenever(catalogue.getDatasetAsync(DatasetNamespace(namespace), DatasetId(datasetId)))
            .thenReturn(completedFuture(dataset))

        throw UnsupportedOperationException("not implemented")
    }

// TODO - always true if Step or non-bucket

    // TODO - true if no dataset in catalogue

    // TODO - true if no etag

    // TODO - true if can't parse etag

    // TODO - true if data not in Howl (contentious)

    // TODO - true if etags mismatch

    // TODO - false if etags match

    // TODO - error if non-404 error from Catalogue

    // TODO - error if non-404 error from Howl
}
