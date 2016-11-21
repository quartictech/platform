package io.quartic.weyl.core.live;

import io.quartic.geojson.Feature;
import io.quartic.geojson.FeatureCollection;
import io.quartic.model.LiveEvent;
import io.quartic.weyl.core.feature.FeatureConverter;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.source.SourceUpdate;
import io.quartic.weyl.core.source.SourceUpdateImpl;

import java.util.Collection;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class LiveEventConverter {
    private final FeatureConverter featureConverter;

    public LiveEventConverter(FeatureConverter featureConverter) {
        this.featureConverter = featureConverter;
    }

    public SourceUpdate updateFrom(FeatureCollection featureCollection) {
        return SourceUpdateImpl.of(convertFeatures(featureCollection.features().stream()));
    }

    public SourceUpdate updateFrom(LiveEvent event) {
        return SourceUpdateImpl.of(convertFeatures(getFeatureStream(event)));
    }

    private Stream<Feature> getFeatureStream(LiveEvent event) {
        return event.featureCollection()
                .map(fc -> fc.features().stream())
                .orElse(Stream.empty());
    }

    private Collection<NakedFeature> convertFeatures(Stream<Feature> featureStream) {
        return featureStream
                .filter(f -> f.geometry().isPresent())  // TODO: we should handle null geometries better
                .map(featureConverter::toModel)
                .collect(toList());
    }

}
