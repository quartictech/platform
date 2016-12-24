package io.quartic.terminator

import io.quartic.catalogue.api.TerminationId
import io.quartic.geojson.FeatureCollection
import io.quartic.terminator.api.FeatureCollectionWithTerminationId
import io.quartic.terminator.api.TerminatorService
import rx.Observable
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import javax.ws.rs.NotFoundException

class TerminatorResource(private val catalogueWatcher: CatalogueWatcher) : TerminatorService {
    private val subject: SerializedSubject<FeatureCollectionWithTerminationId, FeatureCollectionWithTerminationId>
            = PublishSubject.create<FeatureCollectionWithTerminationId>().toSerialized()
    val featureCollections: Observable<FeatureCollectionWithTerminationId> get() = subject

    // TODO: validate that IDs are present on each feature?
    override fun postToDataset(id: TerminationId, featureCollection: FeatureCollection) =
            if (catalogueWatcher.terminationIds.contains(id)) {
                subject.onNext(FeatureCollectionWithTerminationId(id, featureCollection))
            } else {
                throw NotFoundException("Dataset $id not found")
            }
}

