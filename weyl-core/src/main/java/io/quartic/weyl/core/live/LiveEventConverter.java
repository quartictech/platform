package io.quartic.weyl.core.live;

import io.quartic.common.serdes.ObjectMappers;
import io.quartic.common.uid.UidGenerator;
import io.quartic.geojson.Feature;
import io.quartic.geojson.FeatureCollection;
import io.quartic.model.LiveEvent;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.model.AbstractNakedFeature;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.source.SourceUpdate;
import io.quartic.weyl.core.utils.GeometryTransformer;

import java.time.Instant;
import java.util.Collection;
import java.util.stream.Stream;

import static io.quartic.weyl.core.source.ConversionUtils.convertToModelAttributes;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class LiveEventConverter {
    private final UidGenerator<LiveEventId> eidGenerator;
    private final GeometryTransformer geometryTransformer;

    public LiveEventConverter(UidGenerator<LiveEventId> eidGenerator) {
        this(eidGenerator, GeometryTransformer.wgs84toWebMercator());
    }

    public LiveEventConverter(UidGenerator<LiveEventId> eidGenerator,
                              GeometryTransformer geometryTransformer) {
        this.eidGenerator = eidGenerator;
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

    private Stream<Feature> getFeatureStream(LiveEvent event) {
        return event.featureCollection()
                .map(fc -> fc.features().stream())
                .orElse(Stream.empty());
    }

    private Collection<AbstractNakedFeature> convertFeatures(Stream<Feature> featureStream) {
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

    private NakedFeature toJts(Feature f) {
        // HACK: we can assume that we've simply filtered out features with null geometries for now
        return NakedFeature.of(
                f.id().get(),
                geometryTransformer.transform(Utils.toJts(f.geometry().get())),
                convertToModelAttributes(ObjectMappers.OBJECT_MAPPER, f.properties())
        );
    }
}
