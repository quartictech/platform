package io.quartic.weyl.core.live;

import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.ImmutableFeature;
import io.quartic.weyl.core.utils.UidGenerator;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LiveImporter {
    private final Collection<EnrichedLiveEvent> events;
    private final UidGenerator<FeatureId> fidGenerator;

    public LiveImporter(Collection<LiveEvent> events, UidGenerator<FeatureId> fidGenerator, UidGenerator<LiveEventId> eidGenerator) {
        this.events = enrichLiveEvents(events, eidGenerator);
        this.fidGenerator = fidGenerator;
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
        return ImmutableFeature.builder()
                .externalId(f.id().get())
                .uid(fidGenerator.get())
                .geometry(Utils.toJts(f.geometry().get()))  // HACK: we can assume that we've simply filtered out features with null geometries for now
                .metadata(f.properties())
                .build();
    }

}
