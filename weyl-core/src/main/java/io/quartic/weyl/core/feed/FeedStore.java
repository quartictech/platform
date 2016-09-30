package io.quartic.weyl.core.feed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.quartic.weyl.core.live.LiveLayerStoreListener;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.LayerId;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FeedStore implements LiveLayerStoreListener {
    public static final FeedIcon FEED_ICON = FeedIcon.of("blue twitter");   // TODO: don't hardcode this
    public static final String FEED_EVENT_KEY = "feedEvent";
    private final ObjectMapper mapper;
    private final List<ElaboratedFeedEvent> events = Lists.newArrayList();
    private int nextSequenceId = 0;

    public FeedStore(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void onLiveLayerEvent(LayerId layerId, AbstractFeature feature) {
        feature.metadata()
                .getOrDefault(FEED_EVENT_KEY, Optional.empty())
                .ifPresent(raw -> {
                    FeedEvent event = convertOrThrow(raw);
                    events.add(ElaboratedFeedEvent.of(event, layerId, feature.id(), FEED_ICON));
                    nextSequenceId++;
                });
    }

    private FeedEvent convertOrThrow(Object raw) {
        try {
            return mapper.convertValue(raw, FeedEvent.class);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Event information invalid", e);
        }
    }

    public synchronized List<AbstractElaboratedFeedEvent> getEvents(Collection<LayerId> layerIds) {
        return getEventsSince(layerIds, SequenceId.of(0));
    }

    public synchronized List<AbstractElaboratedFeedEvent> getEventsSince(Collection<LayerId> layerIds, SequenceId sequenceId) {
        if (sequenceId.id() > nextSequenceId) {
            return ImmutableList.of();
        }

        return events.subList(sequenceId.id(), events.size())
                .stream()
                .filter(e -> layerIds.contains(e.layerId()))    // TODO: we need to make this more efficient
                .collect(Collectors.toList());
    }

    public synchronized SequenceId getNextSequenceId() {
        return SequenceId.of(nextSequenceId);
    }
}
