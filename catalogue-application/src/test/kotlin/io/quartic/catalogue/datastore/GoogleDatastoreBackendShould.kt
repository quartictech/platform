package io.quartic.catalogue.datastore

import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.Entity
import com.google.cloud.datastore.PathElement
import com.google.cloud.datastore.testing.LocalDatastoreHelper
import io.quartic.catalogue.StorageBackendTests
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.joda.time.Duration
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class GoogleDatastoreBackendShould : StorageBackendTests() {
    private lateinit var datastore: Datastore
    lateinit override var backend: GoogleDatastoreBackend

    @Before
    fun setUp() {
        helper!!.reset()
        datastore = helper!!.options
                .toBuilder()
                .setNamespace("test")
                .build().service
        backend = GoogleDatastoreBackend(datastore)
    }

    @Test
    fun respect_datastore_namespace_separation() {
        val secondBackend = GoogleDatastoreBackend(helper!!.options
                .toBuilder()
                .setNamespace("test2")
                .build().service)

        backend[coords("namespace", "A")] = dataset("1")
        secondBackend[coords("namespace", "A")] = dataset("2")

        val datasets = backend.getAll()
        val secondDatasets = secondBackend.getAll()

        assertThat(datasets.size, equalTo(1))
        assertThat(secondDatasets.size, equalTo(1))

        assertThat(datasets[coords("namespace", "A")]?.metadata?.name, equalTo("1"))
        assertThat(secondDatasets[coords("namespace", "A")]?.metadata?.name, equalTo("2"))
    }

    @Test
    fun ignore_unnamespaced_datasets() {
        // This is where a v1 dataset would be
        val key = datastore.newKeyFactory()
                .addAncestors(PathElement.of("catalogue", "ancestor"))
                .setKind("dataset")
                .newKey("foobles")
        datastore.put(Entity.newBuilder(key).build())

        assertThat(backend.getAll().entries, empty())
    }

    companion object {
        private var helper: LocalDatastoreHelper? = null

        @BeforeClass
        @JvmStatic
        fun setupEmulator() {
            helper = LocalDatastoreHelper.create()
            helper!!.start()
        }

        @AfterClass
        @JvmStatic
        fun tearDownEmulator() {
            helper!!.stop(Duration.millis(3000))
        }
    }
}
