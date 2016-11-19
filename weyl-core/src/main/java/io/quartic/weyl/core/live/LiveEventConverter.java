package io.quartic.weyl.core.live;

import io.quartic.common.serdes.ObjectMappers;
import io.quartic.geojson.Feature;
import io.quartic.geojson.FeatureCollection;
import io.quartic.model.LiveEvent;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.NakedFeatureImpl;
import io.quartic.weyl.core.source.SourceUpdate;
import io.quartic.weyl.core.source.SourceUpdateImpl;
import io.quartic.weyl.core.utils.GeometryTransformer;

import java.util.Collection;
import java.util.stream.Stream;

import static io.quartic.weyl.core.source.ConversionUtils.convertToModelAttributes;
import static java.util.stream.Collectors.toList;

public class LiveEventConverter {
    private final GeometryTransformer geometryTransformer;

    public LiveEventConverter() {
        this(GeometryTransformer.wgs84toWebMercator());
    }

    public LiveEventConverter(GeometryTransformer geometryTransformer) {
        this.geometryTransformer = geometryTransformer;
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
                .map(this::toJts)
                .collect(toList());
    }

    private NakedFeature toJts(Feature f) {
        // HACK: we can assume that we've simply filtered out features with null geometries for now
        return NakedFeatureImpl.of(
                f.id().get(),
                geometryTransformer.transform(Utils.toJts(f.geometry().get())),
                convertToModelAttributes(ObjectMappers.OBJECT_MAPPER, f.properties())
        );
    }
}
