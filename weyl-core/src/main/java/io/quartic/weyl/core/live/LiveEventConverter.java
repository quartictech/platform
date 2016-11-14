package io.quartic.weyl.core.live;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.geojson.FeatureCollection;
import io.quartic.model.LiveEvent;
import io.quartic.weyl.common.uid.UidGenerator;
import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.ImmutableFeature;
import io.quartic.weyl.core.source.ConversionUtils;
import io.quartic.weyl.core.source.SourceUpdate;
import io.quartic.weyl.core.utils.GeometryTransformer;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class LiveEventConverter {
    private final UidGenerator<FeatureId> fidGenerator;
    private final UidGenerator<LiveEventId> eidGenerator;
    private final GeometryTransformer geometryTransformer;
    private final ObjectMapper objectMapper;

    public LiveEventConverter(UidGenerator<FeatureId> fidGenerator, UidGenerator<LiveEventId> eidGenerator, ObjectMapper objectMapper) {
        this(fidGenerator, eidGenerator, objectMapper, GeometryTransformer.wgs84toWebMercator());
    }

    public LiveEventConverter(UidGenerator<FeatureId> fidGenerator, UidGenerator<LiveEventId> eidGenerator,
                              ObjectMapper objectMapper, GeometryTransformer geometryTransformer) {
        this.fidGenerator = fidGenerator;
        this.eidGenerator = eidGenerator;
        this.objectMapper = objectMapper;
        this.geometryTransformer = geometryTransformer;
    }

    public SourceUpdate updateFrom(FeatureCollection featureCollection) {
        return SourceUpdate.of(
                convertFeatures(featureCollection.features().stream()),
                emptyList());
    }

    public SourceUpdate updateFrom(LiveEvent event) {
        return SourceUpdate.of(
                convertFeatures(getFeatureStream(event)),
                enrichEvents(event));
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

    private Collection<EnrichedFeedEvent> enrichEvents(LiveEvent event) {
        final LiveEventId eventId = eidGenerator.get();
        final Instant timestamp = event.timestamp();

        return event.feedEvent()
                .map(feedEvent -> Stream.of(EnrichedFeedEvent.of(eventId, timestamp, feedEvent)))
                .orElse(Stream.empty())
                .collect(toList());
    }


    private io.quartic.weyl.core.model.Feature toJts(io.quartic.geojson.Feature f) {
        // HACK: we can assume that we've simply filtered out features with null geometries for now
        Geometry transformed = geometryTransformer.transform(Utils.toJts(f.geometry().get()));

        return ImmutableFeature.builder()
                .externalId(f.id().get())
                .uid(fidGenerator.get())
                .geometry(transformed)
                .metadata(ConversionUtils.convertMetadata(objectMapper, f.properties()))
                .build();
    }
}
