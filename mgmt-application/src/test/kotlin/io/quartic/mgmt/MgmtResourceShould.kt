package io.quartic.mgmt

import com.nhaarman.mockito_kotlin.*
import io.quartic.catalogue.api.CatalogueService
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetCoordinates
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.api.model.DatasetNamespace
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

class MgmtResourceShould {
    private val namespace = mock<DatasetNamespace>()
    private val catalogue = mock<CatalogueService>()
    private val resource = MgmtResource(catalogue, mock(), namespace)

    @Test
    fun use_default_namespace_when_deleting_dataset() {
        resource.deleteDataset(mock())

        verify(catalogue).deleteDataset(eq(namespace), any())
    }

    @Test
    fun use_default_namespace_when_creating_dataset() {
        whenever(catalogue.registerDataset(any(), any())).thenReturn(mock())

        val request = mock<CreateDatasetRequest> {
            on { accept<DatasetConfig>(any()) } doReturn mock<DatasetConfig>()
        }

        resource.createDataset(request)

        verify(catalogue).registerDataset(eq(namespace), any())
    }

    @Test
    fun filter_retrieved_datasets_to_only_default_namespace() {
        val idA = mock<DatasetId>()
        val idB = mock<DatasetId>()
        val datasetA = mock<DatasetConfig>()
        val datasetB = mock<DatasetConfig>()

        whenever(catalogue.getDatasets()).thenReturn(mapOf(
                DatasetCoordinates(namespace, idA) to datasetA,
                DatasetCoordinates(namespace, idB) to datasetB,
                DatasetCoordinates(mock(), mock()) to mock()
        ))

        assertThat(resource.datasets, equalTo(mapOf(
                idA to datasetA,
                idB to datasetB
        )))
    }
}