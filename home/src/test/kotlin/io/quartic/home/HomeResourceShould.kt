package io.quartic.home

import com.nhaarman.mockito_kotlin.*
import io.quartic.catalogue.api.CatalogueClient
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetId
import io.quartic.catalogue.api.model.DatasetNamespace
import io.quartic.common.auth.User
import io.quartic.common.model.CustomerId
import io.quartic.common.test.assertThrows
import io.quartic.eval.api.EvalQueryServiceClient
import io.quartic.eval.api.EvalTriggerServiceClient
import io.quartic.eval.api.model.ApiDag
import io.quartic.home.howl.HowlStreamingClient
import io.quartic.home.model.*
import io.quartic.home.resource.HomeResource
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

    private val catalogue = mock<CatalogueClient>()
    private val howl = mock<HowlStreamingClient>()
    private val registry = mock<RegistryServiceClient>()
    private val evalQuery = mock<EvalQueryServiceClient>()
    private val evalTrigger = mock<EvalTriggerServiceClient>()

    private val resource = HomeResource(catalogue, howl, evalQuery, evalTrigger, registry)

    @Before
    fun before() {
        whenever(catalogue.getDatasetsAsync()).thenReturn(completedFuture(datasets))
        whenever(registry.getCustomerByIdAsync(CustomerId(5678))).thenReturn(completedFuture(quartic))
    }

    @Test
    fun get_only_authorised_datasets() {
        assertThat(resource.getDatasets(arlo), equalTo(mapOf(foo to datasets[foo])))
    }

    @Test
    fun create_dataset() {
        whenever(catalogue.registerDatasetAsync(any(), any())).thenReturn(completedFuture(mock()))
        whenever(howl.downloadManagedFile(any(), any(), any())).thenReturn("blah".byteInputStream())

        resource.createDataset(arlo, CreateDatasetRequest(mock(), "yeah"))

        verify(catalogue).registerDatasetAsync(eq(foo), any())

        // TODO - validate interaction with Howl, and 2nd param to registerDataset
    }

    @Test
    fun delete_dataset_if_namespace_authorised_and_dataset_exists() {
        whenever(catalogue.deleteDatasetAsync(any(), any())).thenReturn(completedFuture(mock()))

        resource.deleteDataset(arlo, DatasetId("a"))
        verify(catalogue).deleteDatasetAsync(foo, DatasetId("a"))
    }

    @Test
    fun respond_with_404_if_deletion_target_id_not_present() {
        assertThrows<NotFoundException> {
            resource.deleteDataset(arlo, DatasetId("made-up"))
        }
    }

    // TODO - something for uploadFile

    @Test
    fun get_dag_in_cytoscape_format() {
        whenever(evalQuery.getDagAsync(arlo.customerId!!, 69)).thenReturn(completedFuture(
            ApiDag(
                listOf(
                    ApiDag.Node("test", "A", emptyList()),
                    ApiDag.Node("test", "B", emptyList()),
                    ApiDag.Node("test", "C", emptyList()),
                    ApiDag.Node("test", "D", listOf(0, 1)),
                    ApiDag.Node("test", "E", listOf(1, 2, 3))
                )
            )
        ))

        assertThat(resource.getDag(arlo, 69), equalTo(
            CytoscapeDag(
                setOf(
                    node("A", "raw"),
                    node("B", "raw"),
                    node("C", "raw"),
                    node("D", "derived"),
                    node("E", "derived")
                ),
                setOf(
                    edge(0, "A", "D"),
                    edge(1, "B", "D"),
                    edge(2, "B", "E"),
                    edge(3, "C", "E"),
                    edge(4, "D", "E")
                )
            )
        ))
    }

    @Test
    fun show_nothing_for_null_namespace_in_dag() {
        whenever(evalQuery.getDagAsync(arlo.customerId!!, 69)).thenReturn(completedFuture(
            ApiDag(
                listOf(
                    ApiDag.Node(null, "A", emptyList())     // Null!
                )
            )
        ))

        assertThat(resource.getDag(arlo, 69), equalTo(
            CytoscapeDag(
                setOf(
                    node("A", "raw", "")        // Null becomes empty string
                ),
                emptySet()
            )
        ))
    }

    private fun node(id: String, type: String, namespace: String = "test") =
        CytoscapeNode(CytoscapeNodeData("${namespace}::${id}", "${namespace}::${id}", type))

    private fun edge(id: Long, source: String, target: String, namespace: String = "test") =
        CytoscapeEdge(CytoscapeEdgeData(id, "${namespace}::${source}", "${namespace}::${target}"))


}
