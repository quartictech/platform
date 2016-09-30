package io.quartic.weyl.core.feed;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.LayerId;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

import static io.quartic.weyl.core.feed.FeedStore.FEED_EVENT_KEY;
import static io.quartic.weyl.core.feed.FeedStore.FEED_ICON;
import static io.quartic.weyl.core.utils.Utils.uuid;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class FeedStoreShould {
    private final FeedStore store = new FeedStore();
    private final LayerId layerId = LayerId.of("abcd");

    @Test
    public void accept_features_with_events() throws Exception {
        final SequenceId initialSequenceId = store.getNextSequenceId();
        store.onLiveLayerEvent(layerId, featureWithEvent());

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
        final AbstractFeature featureA = featureWithEvent();
        final AbstractFeature featureB = featureWithEvent();

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
        final AbstractFeature featureA = featureWithEvent();
        final AbstractFeature featureB = featureWithEvent();

        store.onLiveLayerEvent(layerId, featureA);
        store.onLiveLayerEvent(otherLayerId, featureB);

        assertThat(
                store.getEvents(ImmutableList.of(layerId)),
                equalTo(ImmutableList.of(elaborate(featureA, layerId)))
        );
    }

    @Test
    public void only_retrieve_events_after_specified_sequence_id() throws Exception {
        final AbstractFeature featureA = featureWithEvent();
        final AbstractFeature featureB = featureWithEvent();

        store.onLiveLayerEvent(layerId, featureWithEvent());
        store.onLiveLayerEvent(layerId, featureWithEvent());
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
        store.onLiveLayerEvent(layerId, featureWithEvent());
        store.onLiveLayerEvent(layerId, featureWithEvent());
        final SequenceId seqId = store.getNextSequenceId();

        assertThat(
                store.getEventsSince(ImmutableList.of(layerId), SequenceId.of(seqId.id() + 1)),
                empty()
        );
    }

    private AbstractFeature featureWithEvent() {
        return feature(ImmutableMap.of(FEED_EVENT_KEY, Optional.of(mock(AbstractFeedEvent.class))));
    }

    private AbstractFeature featureWithoutEvent() {
        return feature(ImmutableMap.of());
    }

    private AbstractFeature feature(Map<String, Optional<Object>> properties) {
        return Feature.of(
                uuid(FeatureId::of),
                mock(Geometry.class),
                properties);
    }

    private ElaboratedFeedEvent elaborate(AbstractFeature feature, LayerId layerId) {
        return ElaboratedFeedEvent.of(
                (AbstractFeedEvent)feature.metadata().get(FEED_EVENT_KEY).get(),
                layerId,
                feature.id(),
                FEED_ICON);
    }
}
