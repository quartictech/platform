package io.quartic.mgmt

import com.nhaarman.mockito_kotlin.*
import io.quartic.catalogue.api.CatalogueService
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.howl.api.HowlService
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.util.*

class MgmtResourceShould {
    private val namespace = mock<DatasetNamespace>()
    private val catalogue = mock<CatalogueService>()
    private val howl = mock<HowlService>()
    private val resource = MgmtResource(catalogue, howl, namespace)

    @Test
    fun use_default_namespace_when_deleting_dataset() {
        resource.deleteDataset(mock())

        verify(catalogue).deleteDataset(eq(namespace), any())
    }

    @Test
    fun use_default_namespace_when_creating_dataset() {
        whenever(catalogue.registerDataset(any(), any())).thenReturn(mock())
        whenever(howl.downloadFile(any(), any())).thenReturn(Optional.of("blah".byteInputStream()))

        resource.createDataset(CreateStaticDatasetRequest(mock(), "foo", FileType.RAW))

        verify(catalogue).registerDataset(eq(namespace), any())
    }

    @Test
    fun retrieve_only_datasets_from_default_namespace() {
        val idA = mock<DatasetId>()
        val idB = mock<DatasetId>()
        val datasetA = mock<DatasetConfig>()
        val datasetB = mock<DatasetConfig>()

        whenever(catalogue.getDatasets()).thenReturn(mapOf(
                namespace to mapOf(idA to datasetA, idB to datasetB),
                mock<DatasetNamespace>() to mapOf(mock<DatasetId>() to mock<DatasetConfig>())
        ))

        assertThat(resource.getDatasets(), equalTo(mapOf(idA to datasetA, idB to datasetB)))
    }
}