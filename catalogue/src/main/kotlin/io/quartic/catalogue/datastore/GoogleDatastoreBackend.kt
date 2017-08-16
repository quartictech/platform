package io.quartic.catalogue.datastore

import com.codahale.metrics.health.HealthCheck.Result
import com.google.cloud.datastore.*
import io.quartic.catalogue.StorageBackend
import io.quartic.catalogue.api.model.DatasetConfig
import io.quartic.catalogue.api.model.DatasetCoordinates
import io.quartic.common.logging.logger

/**
 * GoogleDatastoreBackend

 * Note that according to https://cloud.google.com/datastore/docs/concepts/structuring_for_strong_consistency
 * strong consistency requires structuring of your data within Google Datastore. In particular only ancestor queries
 * are guaranteed to be strongly consistent. Since we don't want to have to worry about inconsistent state here, we
 * put all of our dataset entities under an ancestor catalogue entity. This should give us strong consistency according
 * to the docs.
 */
class GoogleDatastoreBackend(private val datastore: Datastore) : StorageBackend {
    private val LOG by logger()
    private val ancestorKey = datastore.newKeyFactory().setKind(ANCESTOR_KIND).newKey(ANCESTOR)
    private val entitySerDe = EntitySerDe { this.key(it) }

    init {
        datastore.put(Entity.newBuilder().setKey(ancestorKey).build())
    }

    override fun get(coords: DatasetCoordinates) = entitySerDe.entityToDataset(datastore.get(key(coords)))

    override fun set(coords: DatasetCoordinates, config: DatasetConfig) {
        datastore.runInTransaction { readerWriter -> readerWriter.put(entitySerDe.datasetToEntity(coords, config)) }
    }

    override fun remove(coords: DatasetCoordinates) {
        datastore.delete(key(coords))
    }

    override fun contains(coords: DatasetCoordinates) = datastore.get(key(coords)) != null

    override fun getAll(): Map<DatasetCoordinates, DatasetConfig> {
        val query = Query.newEntityQueryBuilder()
                .setKind(DATASET_KIND)
                .setFilter(StructuredQuery.PropertyFilter.hasAncestor(ancestorKey))
                .build()

        val datasets = mutableMapOf<DatasetCoordinates, DatasetConfig>()
        val results = datastore.run(query)
        while (results.hasNext()) {
            val entity = results.next()

            if (entity.key.ancestors.size == 1) {
                LOG.info("Skipping un-namespaced dataset '" + entity.key.name + "'")
            } else {
                datasets.put(
                        DatasetCoordinates(entity.key.parent.name, entity.key.name),
                        entitySerDe.entityToDataset(entity)
                )
            }
        }
        return datasets
    }

    override fun healthCheck() = if (datastore.get(ancestorKey) != null) {
        Result.healthy()
    } else {
        Result.unhealthy("can't find ancestor key")
    }

    private fun key(coords: DatasetCoordinates) = datastore.newKeyFactory()
            .addAncestors(
                    PathElement.of(ANCESTOR_KIND, ANCESTOR),
                    PathElement.of(NAMESPACE_KIND, coords.namespace.namespace)
            )
            .setKind(DATASET_KIND)
            .newKey(coords.id.uid)

    companion object {
        private val ANCESTOR = "ancestor"
        private val ANCESTOR_KIND = "catalogue"
        private val NAMESPACE_KIND = "namespace"
        private val DATASET_KIND = "dataset"

        fun remote(projectId: String, namespace: String) = GoogleDatastoreBackend(DatastoreOptions.newBuilder()
                .setNamespace(namespace)
                .setProjectId(projectId)
                .build()
                .service
        )
    }
}
