package io.quartic.terminator;

import io.quartic.catalogue.api.DatasetId;
import io.quartic.geojson.FeatureCollection;
import io.quartic.terminator.api.FeatureCollectionWithDatasetId;
import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/datasets")
public class TerminatorResource {
    private final SerializedSubject<FeatureCollectionWithDatasetId, FeatureCollectionWithDatasetId> subject
            = PublishSubject.<FeatureCollectionWithDatasetId>create().toSerialized();
    private final CatalogueProxy catalogue;

    public TerminatorResource(CatalogueProxy catalogue) {
        this.catalogue = catalogue;
    }

    @POST
    @Path("/{id}")
    public void postToDataset(@PathParam("id") String id, FeatureCollection featureCollection) {
        final DatasetId datasetId = DatasetId.of(id);
        if (catalogue.datasets().containsKey(datasetId)) {
            subject.onNext(FeatureCollectionWithDatasetId.of(datasetId, featureCollection));
        } else {
            throw new NotFoundException("Dataset " + id + " not found");
        }
    }

    Observable<FeatureCollectionWithDatasetId> featureCollectionsWithDatasetIds() {
        return subject;
    }
}
