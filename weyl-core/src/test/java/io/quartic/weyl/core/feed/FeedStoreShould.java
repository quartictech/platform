package io.quartic.weyl.core.feed;

import com.google.common.collect.ImmutableList;
import io.quartic.weyl.core.model.LayerId;
import org.junit.Test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class FeedStoreShould {
    private final FeedStore store = new FeedStore();

    @Test
    public void retrieve_event_for_specified_layer() throws Exception {
        LayerId layerId = LayerId.of("abcd");
        AbstractFeedEvent eventA = event();
        AbstractFeedEvent eventB = event();

        store.pushEvent(layerId, eventA);
        store.pushEvent(layerId, eventB);

        assertThat(
                store.getEvents(ImmutableList.of(layerId)),
                equalTo(ImmutableList.of(eventA, eventB))
        );
    }

    @Test
    public void not_retrieve_events_for_other_layers() throws Exception {
        LayerId layerId = LayerId.of("abcd");
        LayerId otherLayerId = LayerId.of("efgh");
        AbstractFeedEvent eventA = event();
        AbstractFeedEvent eventB = event();

        store.pushEvent(layerId, eventA);
        store.pushEvent(otherLayerId, eventB);

        assertThat(
                store.getEvents(ImmutableList.of(layerId)),
                equalTo(ImmutableList.of(eventA))
        );
    }

    @Test
    public void only_retrieve_events_after_specified_sequence_id() throws Exception {
        LayerId layerId = LayerId.of("abcd");
        AbstractFeedEvent eventA = event();
        AbstractFeedEvent eventB = event();

        store.pushEvent(layerId, event());
        store.pushEvent(layerId, event());
        final SequenceId seqId = store.getNextSequenceId();
        store.pushEvent(layerId, eventA);
        store.pushEvent(layerId, eventB);

        assertThat(
                store.getEventsSince(ImmutableList.of(layerId), seqId),
                equalTo(ImmutableList.of(eventA, eventB))
        );
    }

    @Test
    public void retrive_no_events_if_sequence_id_in_the_future() throws Exception {
        LayerId layerId = LayerId.of("abcd");

        store.pushEvent(layerId, event());
        store.pushEvent(layerId, event());
        final SequenceId seqId = store.getNextSequenceId();

        assertThat(
                store.getEventsSince(ImmutableList.of(layerId), SequenceId.of(seqId.id() + 1)),
                empty()
        );
    }

    private AbstractFeedEvent event() {
        return mock(AbstractFeedEvent.class);
    }
}
