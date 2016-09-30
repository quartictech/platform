package io.quartic.weyl.core.feed;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.quartic.weyl.core.geojson.SweetStyle;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class FeedStore {
    @SweetStyle
    @Value.Immutable
    interface AbstractEventWithLayer {
        LayerId layerId();
        AbstractFeedEvent event();
    }

    private final List<EventWithLayer> events = Lists.newArrayList();
    private int nextSequenceId = 0;

    public synchronized void pushEvent(LayerId layerId, AbstractFeedEvent event) {
        events.add(EventWithLayer.of(layerId, event));
        nextSequenceId++;
    }

    public synchronized List<AbstractFeedEvent> getEvents(Collection<LayerId> layerIds) {
        return getEventsSince(layerIds, SequenceId.of(0));
    }

    public synchronized List<AbstractFeedEvent> getEventsSince(Collection<LayerId> layerIds, SequenceId sequenceId) {
        if (sequenceId.id() > nextSequenceId) {
            return ImmutableList.of();
        }

        return events.subList(sequenceId.id(), events.size())
                .stream()
                .filter(e -> layerIds.contains(e.layerId()))    // TODO: we need to make this more efficient
                .map(EventWithLayer::event)
                .collect(Collectors.toList());
    }

    public synchronized SequenceId getNextSequenceId() {
        return SequenceId.of(nextSequenceId);
    }
}
