package io.quartic.terminator;

import io.quartic.catalogue.api.TerminationId;
import io.quartic.geojson.FeatureCollection;
import io.quartic.terminator.api.FeatureCollectionWithTerminationId;
import io.quartic.terminator.api.TerminatorService;
import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;

import javax.ws.rs.NotFoundException;

public class TerminatorResource implements TerminatorService {
    private final SerializedSubject<FeatureCollectionWithTerminationId, FeatureCollectionWithTerminationId> subject
            = PublishSubject.<FeatureCollectionWithTerminationId>create().toSerialized();
    private final CatalogueWatcher catalogue;

    public TerminatorResource(CatalogueWatcher catalogue) {
        this.catalogue = catalogue;
    }

    @Override
    public void postToDataset(String id, FeatureCollection featureCollection) {
        final TerminationId terminationId = TerminationId.of(id);

        // TODO: validate that IDs are present on each feature?

        if (catalogue.terminationIds().contains(terminationId)) {
            subject.onNext(FeatureCollectionWithTerminationId.of(terminationId, featureCollection));
        } else {
            throw new NotFoundException("Dataset " + id + " not found");
        }
    }

    public Observable<FeatureCollectionWithTerminationId> featureCollections() {
        return subject;
    }
}
