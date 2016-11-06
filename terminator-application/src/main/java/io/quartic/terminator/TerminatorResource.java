package io.quartic.terminator;

import io.quartic.catalogue.api.DatasetId;
import io.quartic.geojson.FeatureCollection;
import io.quartic.terminator.api.FeatureCollectionWithDatasetId;
import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;

import javax.ws.rs.NotFoundException;

public class TerminatorResource implements TerminatorService {
    private final SerializedSubject<FeatureCollectionWithDatasetId, FeatureCollectionWithDatasetId> subject
            = PublishSubject.<FeatureCollectionWithDatasetId>create().toSerialized();
    private final CatalogueProxy catalogue;

    public TerminatorResource(CatalogueProxy catalogue) {
        this.catalogue = catalogue;
    }

    @Override
    public void postToDataset(String id, FeatureCollection featureCollection) {
        final DatasetId datasetId = DatasetId.of(id);
        if (catalogue.datasets().containsKey(datasetId)) {
            subject.onNext(FeatureCollectionWithDatasetId.of(datasetId, featureCollection));
        } else {
            throw new NotFoundException("Dataset " + id + " not found");
        }
    }

    public Observable<FeatureCollectionWithDatasetId> featureCollectionsWithDatasetIds() {
        return subject;
    }
}
