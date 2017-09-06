package io.quartic.catalogue

import io.quartic.catalogue.api.model.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import java.time.Instant
import java.util.Collections.emptyMap

abstract class StorageBackendTests {
    @Test
    fun store_and_retrieve_dataset() {
        val datasetConfig = dataset("name")
        val coords = coords("namespace", "abc")

        backend[coords] = datasetConfig

        assertThat(backend[coords], equalTo(datasetConfig))
    }

    @Test
    fun retrieve_updated_dataset() {
        val coords = coords("namespace", "abc")

        backend[coords] = dataset("Old Name")
        backend[coords] = dataset("New Name")

        assertThat(backend[coords]!!.metadata.name, equalTo("New Name"))
    }

    @Test
    fun fetch_all_datasets() {
        val datasetA = dataset("A")
        val datasetB = dataset("B")
        val datasetC = dataset("C")
        backend[coords("foo", "A")] = datasetA
        backend[coords("foo", "B")] = datasetB
        backend[coords("bar", "C")] = datasetC

        assertThat(backend.getAll(), equalTo(mapOf(
                coords("foo", "A") to datasetA,
                coords("foo", "B") to datasetB,
                coords("bar", "C") to datasetC
        )))
    }

    @Test
    fun enforce_dataset_namespace_isolation() {
        val coordsA = coords("ns1", "abc")
        val coordsB = coords("ns21", "abc")
        val datasetA = dataset("foo")
        val datasetB = dataset("bar")

        backend[coordsA] = datasetA
        backend[coordsB] = datasetB

        assertThat(backend[coordsA], equalTo(datasetA))
        assertThat(backend[coordsB], equalTo(datasetB))
    }

    @Test
    fun contains() {
        val coords = coords("namespace", "abc")

        backend[coords] = dataset("A")

        assertThat(backend.contains(coords), equalTo(true))
    }

    @Test
    fun remove() {
        val dataset = dataset("foo")
        val coords = coords("namespace", "abc")

        backend[coords] = dataset
        backend.remove(coords)

        assertThat(backend.getAll(), equalTo(emptyMap()))
    }

    protected fun dataset(name: String): DatasetConfig {
        val metadata = DatasetMetadata(
                name,
                "description",
                "attribution",
                Instant.now())
        val extensions = mapOf("A" to "B")
        val locator = DatasetLocator.CloudDatasetLocator("WAT", false, MimeType.RAW)
        return DatasetConfig(metadata, locator, extensions)
    }

    protected abstract val backend: StorageBackend

    protected fun coords(namespace: String, id: String) = DatasetCoordinates(namespace, id)
}
