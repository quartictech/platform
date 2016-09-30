package io.quartic.weyl.core.feed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.live.LiveLayerStore;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.LayerId;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import static io.quartic.weyl.core.feed.FeedStore.FEED_EVENT_KEY;
import static io.quartic.weyl.core.feed.FeedStore.FEED_ICON;
import static io.quartic.weyl.core.utils.Utils.idSupplier;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class FeedStoreShould {
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule());
    private final FeedStore store = new FeedStore(mock(LiveLayerStore.class), mapper, () -> "xyz");
    private final LayerId layerId = LayerId.of("abcd");

    @Test
    public void accept_features_with_events() throws Exception {
        final SequenceId initialSequenceId = store.getNextSequenceId();
        store.onLiveLayerEvent(layerId, featureWithEvent("alice"));

        assertThat(
                store.getNextSequenceId(),
                not(equalTo(initialSequenceId)));
    }

    @Test
    public void ignore_features_without_events() throws Exception {
        final SequenceId initialSequenceId = store.getNextSequenceId();
        store.onLiveLayerEvent(layerId, featureWithoutEvent());

        assertThat(
                store.getNextSequenceId(),
                equalTo(initialSequenceId));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_if_invalid_event_specified() throws Exception {
        store.onLiveLayerEvent(layerId, feature(ImmutableMap.of(FEED_EVENT_KEY, Optional.of(new Object()))));
    }

    @Test
    public void retrieve_event_for_specified_layer() throws Exception {
        final AbstractFeature featureA = featureWithEvent("alice");
        final AbstractFeature featureB = featureWithEvent("bob");

        store.onLiveLayerEvent(layerId, featureA);
        store.onLiveLayerEvent(layerId, featureB);

        assertThat(
                store.getEvents(ImmutableList.of(layerId)),
                equalTo(ImmutableList.of(
                        elaborate(featureA, layerId),
                        elaborate(featureB, layerId)
                ))
        );
    }

    @Test
    public void not_retrieve_events_for_other_layers() throws Exception {
        LayerId otherLayerId = LayerId.of("efgh");
        final AbstractFeature featureA = featureWithEvent("alice");
        final AbstractFeature featureB = featureWithEvent("bob");

        store.onLiveLayerEvent(layerId, featureA);
        store.onLiveLayerEvent(otherLayerId, featureB);

        assertThat(
                store.getEvents(ImmutableList.of(layerId)),
                equalTo(ImmutableList.of(elaborate(featureA, layerId)))
        );
    }

    @Test
    public void only_retrieve_events_after_specified_sequence_id() throws Exception {
        final AbstractFeature featureA = featureWithEvent("alice");
        final AbstractFeature featureB = featureWithEvent("bob");

        store.onLiveLayerEvent(layerId, featureWithEvent("charlie"));
        store.onLiveLayerEvent(layerId, featureWithEvent("doug"));
        final SequenceId seqId = store.getNextSequenceId();
        store.onLiveLayerEvent(layerId, featureA);
        store.onLiveLayerEvent(layerId, featureB);

        assertThat(
                store.getEventsSince(ImmutableList.of(layerId), seqId),
                equalTo(ImmutableList.of(
                        elaborate(featureA, layerId),
                        elaborate(featureB, layerId)
                ))
        );
    }

    @Test
    public void retrive_no_events_if_sequence_id_in_the_future() throws Exception {
        store.onLiveLayerEvent(layerId, featureWithEvent("alice"));
        store.onLiveLayerEvent(layerId, featureWithEvent("bob"));
        final SequenceId seqId = store.getNextSequenceId();

        assertThat(
                store.getEventsSince(ImmutableList.of(layerId), SequenceId.of(seqId.id() + 1)),
                empty()
        );
    }

    private AbstractFeature featureWithEvent(String user) {
        return feature(ImmutableMap.of(
                FEED_EVENT_KEY, Optional.of(mapper.convertValue(event(user), Map.class))));
    }

    private AbstractFeature featureWithoutEvent() {
        return feature(ImmutableMap.of());
    }

    private AbstractFeature feature(Map<String, Optional<Object>> properties) {
        return Feature.of(
                FeatureId.of(idSupplier().get()),
                mock(Geometry.class),
                properties);
    }

    private FeedEvent event(String user) {
        return FeedEvent.of(
                FeedUser.of(user),
                ZonedDateTime.of(2016, 12, 25, 5, 6, 7, 0, ZoneId.of("Europe/London")),
                "Lorem ipsum."
        );
    }

    private ElaboratedFeedEvent elaborate(AbstractFeature feature, LayerId layerId) {
        return ElaboratedFeedEvent.of(
                FeedEventId.of("xyz"),
                mapper.convertValue(feature.metadata().get(FEED_EVENT_KEY).get(), FeedEvent.class),
                layerId,
                feature.id(),
                FEED_ICON);
    }
}
