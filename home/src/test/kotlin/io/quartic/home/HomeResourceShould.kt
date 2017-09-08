package io.quartic.home

import com.nhaarman.mockito_kotlin.*
import io.quartic.catalogue.api.CatalogueService
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.common.auth.User
import io.quartic.common.model.CustomerId
import io.quartic.common.test.assertThrows
import io.quartic.eval.api.EvalQueryServiceClient
import io.quartic.home.resource.HomeResource
import io.quartic.howl.api.HowlService
import io.quartic.registry.api.RegistryServiceClient
import io.quartic.registry.api.model.Customer
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CompletableFuture.completedFuture
import javax.ws.rs.NotFoundException

class HomeResourceShould {

    private val arlo = User(1234, 5678)
    private val quartic = Customer(
        CustomerId(5678),
        githubOrgId = 1,
        githubRepoId = 1,
        githubInstallationId = 123,
        name = "quartic",
        namespace = "foo",
        subdomain = "foo"
    )

    private val foo = DatasetNamespace("foo")
    private val bar = DatasetNamespace("bar")

    private val datasets = mapOf(
            foo to mapOf(DatasetId("a") to mock<DatasetConfig>(), DatasetId("b") to mock<DatasetConfig>()),
            bar to mapOf(DatasetId("c") to mock<DatasetConfig>(), DatasetId("e") to mock<DatasetConfig>())
    )

    private val catalogue = mock<CatalogueService>()
    private val howl = mock<HowlService>()
    private val registry = mock<RegistryServiceClient>()
    private val eval = mock<EvalQueryServiceClient>()

    private val resource = HomeResource(catalogue, howl, eval, registry)

    @Before
    fun before() {
        whenever(catalogue.getDatasets()).thenReturn(datasets)
        whenever(registry.getCustomerByIdAsync(CustomerId(5678))).thenReturn(completedFuture(quartic))
    }

    @Test
    fun get_only_authorised_datasets() {
        assertThat(resource.getDatasets(arlo), equalTo(mapOf(foo to datasets[foo])))
    }

    @Test
    fun create_dataset_if_namespace_authorised() {
        whenever(catalogue.registerDataset(any(), any())).thenReturn(mock())
        whenever(howl.downloadFile(any(), any())).thenReturn("blah".byteInputStream())

        resource.createDataset(arlo, foo, CreateStaticDatasetRequest(mock(), "yeah", FileType.RAW))

        verify(catalogue).registerDataset(eq(foo), any())

        // TODO - validate interaction with Howl, and 2nd param to registerDataset
    }

    @Test
    fun not_create_dataset_and_respond_with_404_if_namespace_not_authorised() {
        assertThrows<NotFoundException> {
            resource.createDataset(arlo, bar, CreateStaticDatasetRequest(mock(), "yeah", FileType.RAW))
        }

        verifyNoMoreInteractions(catalogue)
    }

    @Test
    fun delete_dataset_if_namespace_authorised_and_dataset_exists() {
        resource.deleteDataset(arlo, foo, DatasetId("a"))
        verify(catalogue).deleteDataset(foo, DatasetId("a"))
    }

    @Test
    fun not_delete_dataset_and_respond_with_404_if_namespace_not_authorised() {
        assertThrows<NotFoundException> {
            resource.deleteDataset(arlo, foo, DatasetId("d"))   // In unauthorised namespace
        }

        verify(catalogue, never()).deleteDataset(any(), any())
    }

    @Test
    fun respond_with_404_if_deletion_target_namespace_not_present() {
        assertThrows<NotFoundException> {
            resource.deleteDataset(arlo, DatasetNamespace("made-up"), DatasetId("a"))
        }
    }

    @Test
    fun respond_with_404_if_deletion_target_id_not_present() {
        assertThrows<NotFoundException> {
            resource.deleteDataset(arlo, foo, DatasetId("made-up"))
        }
    }

    // TODO - something for uploadFile
}
