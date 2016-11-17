package io.quartic.weyl.core.live;

import com.vividsolutions.jts.geom.Geometry;
import io.quartic.common.serdes.ObjectMappers;
import io.quartic.common.uid.UidGenerator;
import io.quartic.geojson.FeatureCollection;
import io.quartic.model.LiveEvent;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.ImmutableFeature;
import io.quartic.weyl.core.source.ConversionUtils;
import io.quartic.weyl.core.source.SourceUpdate;
import io.quartic.weyl.core.utils.GeometryTransformer;

import java.util.Collection;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class LiveEventConverter {
    private final UidGenerator<FeatureId> fidGenerator;
    private final GeometryTransformer geometryTransformer;

    public LiveEventConverter(UidGenerator<FeatureId> fidGenerator) {
        this(fidGenerator, GeometryTransformer.wgs84toWebMercator());
    }

    public LiveEventConverter(UidGenerator<FeatureId> fidGenerator, GeometryTransformer geometryTransformer) {
        this.fidGenerator = fidGenerator;
        this.geometryTransformer = geometryTransformer;
    }

    public SourceUpdate updateFrom(FeatureCollection featureCollection) {
        return SourceUpdate.of(convertFeatures(featureCollection.features().stream()));
    }

    public SourceUpdate updateFrom(LiveEvent event) {
        return SourceUpdate.of(convertFeatures(getFeatureStream(event)));
    }

    private Stream<io.quartic.geojson.Feature> getFeatureStream(LiveEvent event) {
        return event.featureCollection()
                .map(fc -> fc.features().stream())
                .orElse(Stream.empty());
    }

    private Collection<Feature> convertFeatures(Stream<io.quartic.geojson.Feature> featureStream) {
        return featureStream
                .filter(f -> f.geometry().isPresent())  // TODO: we should handle null geometries better
                .map(this::toJts)
                .collect(toList());
    }

    private io.quartic.weyl.core.model.Feature toJts(io.quartic.geojson.Feature f) {
        // HACK: we can assume that we've simply filtered out features with null geometries for now
        Geometry transformed = geometryTransformer.transform(Utils.toJts(f.geometry().get()));

        return ImmutableFeature.builder()
                .externalId(f.id().get())
                .uid(fidGenerator.get())
                .geometry(transformed)
                .attributes(ConversionUtils.convertAttributes(ObjectMappers.OBJECT_MAPPER, f.properties()))
                .build();
    }
}
