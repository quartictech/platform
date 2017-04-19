package io.quartic.mgmt

import com.nhaarman.mockito_kotlin.*
import io.quartic.catalogue.api.CatalogueService
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.api.model.DatasetNamespace
import org.junit.Test

class MgmtResourceShould {
    private val namespace = DatasetNamespace("foo")
    private val catalogue = mock<CatalogueService>()
    private val resource = MgmtResource(catalogue, mock(), namespace)

    @Test
    fun use_default_namespace_when_deleting_dataset() {
        resource.deleteDataset(DatasetId("123"))

        verify(catalogue).deleteDataset(eq(namespace), any())
    }

    @Test
    fun use_default_namespace_when_creating_dataset() {
        val request = mock<CreateDatasetRequest> {
            on { accept<DatasetConfig>(any()) } doReturn mock<DatasetConfig>()
        }

        resource.createDataset(request)

        verify(catalogue).registerDataset(eq(namespace), any())
    }

    @Test
    fun use_default_namespace_when_getting_datasets() {
        resource.datasets

        verify(catalogue).getDatasets(namespace)
    }
}