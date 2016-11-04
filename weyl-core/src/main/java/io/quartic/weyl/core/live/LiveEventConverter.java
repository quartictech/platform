package io.quartic.weyl.core.live;

import com.vividsolutions.jts.geom.Geometry;
import io.quartic.model.FeedEvent;
import io.quartic.model.LiveEvent;
import io.quartic.weyl.common.uid.UidGenerator;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.ImmutableFeature;
import io.quartic.weyl.core.source.SourceUpdate;
import io.quartic.weyl.core.utils.GeometryTransformer;

import java.util.Collection;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class LiveEventConverter {
    private final UidGenerator<FeatureId> fidGenerator;
    private final UidGenerator<LiveEventId> eidGenerator;
    private final GeometryTransformer geometryTransformer;

    public LiveEventConverter(UidGenerator<FeatureId> fidGenerator, UidGenerator<LiveEventId> eidGenerator) {
        this(fidGenerator, eidGenerator, GeometryTransformer.wgs84toWebMercator());
    }

    public LiveEventConverter(UidGenerator<FeatureId> fidGenerator, UidGenerator<LiveEventId> eidGenerator, GeometryTransformer geometryTransformer) {
        this.fidGenerator = fidGenerator;
        this.eidGenerator = eidGenerator;
        this.geometryTransformer = geometryTransformer;
    }

    public SourceUpdate toUpdate(LiveEvent event) {
        final EnrichedLiveEvent enriched = EnrichedLiveEvent.of(eidGenerator.get(), event);
        return SourceUpdate.of(getFeatures(enriched), getFeedEvents(enriched));
    }

    private Collection<Feature> getFeatures(EnrichedLiveEvent event) {
        return event.liveEvent().featureCollection()
                .map(fc -> fc.features().stream()).orElse(Stream.empty())
                .filter(f -> f.geometry().isPresent())  // TODO: we should handle null geometries better
                .map(this::toJts)
                .collect(toList());
    }

    private Collection<EnrichedFeedEvent> getFeedEvents(EnrichedLiveEvent event) {
        return event.liveEvent().feedEvent()
                .map(feedEvent -> Stream.of(enrichedFeedEvent(event, feedEvent)))
                .orElse(Stream.empty())
                .collect(toList());
    }

    private static EnrichedFeedEvent enrichedFeedEvent(EnrichedLiveEvent liveEvent, FeedEvent feedEvent) {
        return EnrichedFeedEvent.of(liveEvent.eventId(), liveEvent.liveEvent().timestamp(), feedEvent);
    }

    private io.quartic.weyl.core.model.Feature toJts(io.quartic.geojson.Feature f) {
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
