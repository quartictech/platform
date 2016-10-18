package io.quartic.weyl.core.live;

import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.ImmutableFeature;
import io.quartic.weyl.core.utils.GeometryTransformer;
import io.quartic.weyl.core.utils.UidGenerator;
import org.opengis.referencing.FactoryException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LiveImporter {
    private final Collection<EnrichedLiveEvent> events;
    private final UidGenerator<FeatureId> fidGenerator;
    private final GeometryTransformer geometryTransformer;

    public LiveImporter(Collection<LiveEvent> events, UidGenerator<FeatureId> fidGenerator,
                        UidGenerator<LiveEventId> eidGenerator, GeometryTransformer geometryTransformer) {
        this.events = enrichLiveEvents(events, eidGenerator);
        this.fidGenerator = fidGenerator;
        this.geometryTransformer = geometryTransformer;
    }

    public LiveImporter(List<LiveEvent> events, UidGenerator<FeatureId> fidGenerator, UidGenerator<LiveEventId> eidGenerator) {
        this(events, fidGenerator, eidGenerator, GeometryTransformer.wgs84toWebMercator());
    }

    public Collection<EnrichedFeedEvent> getFeedEvents() {
        return events.stream()
                .flatMap(event -> event.liveEvent().feedEvent()
                        .map(feedEvent -> Stream.of(enrichedFeedEvent(event, feedEvent)))
                        .orElse(Stream.empty())
                )
                .collect(Collectors.toList());
    }

    public Collection<Feature> getFeatures() {
        return events.stream()
                .flatMap(event -> event.liveEvent().featureCollection().map(fc -> fc.features().stream()).orElse(Stream.empty()))
                .filter(f -> f.geometry().isPresent())  // TODO: we should handle null geometries better
                .map(this::toJts)
                .collect(Collectors.toList());
    }

    private Collection<EnrichedLiveEvent> enrichLiveEvents(Collection<LiveEvent> events, UidGenerator<LiveEventId> eidGenerator) {
        return events.stream()
                .map(event -> EnrichedLiveEvent.of(eidGenerator.get(), event))
                .collect(Collectors.toList());
    }

    private static EnrichedFeedEvent enrichedFeedEvent(EnrichedLiveEvent liveEvent, FeedEvent feedEvent) {
        return EnrichedFeedEvent.of(liveEvent.eventId(), liveEvent.liveEvent().timestamp(), feedEvent);
    }

    private io.quartic.weyl.core.model.Feature toJts(io.quartic.weyl.core.geojson.Feature f) {
        // HACK: we can assume that we've simply filtered out features with null geometries for now
        Geometry transformed = geometryTransformer.transform(Utils.toJts(f.geometry().get()));

        return ImmutableFeature.builder()
                .externalId(f.id().get())
                .uid(fidGenerator.get())
                .geometry(transformed)
                .metadata(f.properties())
                .build();
    }

}
