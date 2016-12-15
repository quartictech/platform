package io.quartic.weyl.update;

import io.quartic.weyl.Multiplexer;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.ClientStatusMessage.GeofenceStatus;
import io.quartic.weyl.websocket.message.SelectionDrivenUpdateMessageImpl;
import io.quartic.weyl.websocket.message.SelectionStatusImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.empty;
import static rx.Observable.just;

public class SelectionHandlerShould {
    private final Multiplexer<Integer, EntityId, Feature> mux = mock(Multiplexer.class);
    private final SelectionDrivenUpdateGenerator generator = mock(SelectionDrivenUpdateGenerator.class);

    @Before
    public void before() throws Exception {
        when(mux.call(any())).thenReturn(empty());
    }

    @Test
    public void route_entity_list_to_mux() throws Exception {
        final List<EntityId> ids = newArrayList(mock(EntityId.class));
        final TestSubscriber<Pair<Integer, List<EntityId>>> subscriber = subscriberFromMux();

        just(status(ids)).compose(handler()).subscribe();

        subscriber.awaitValueCount(1, 100, MILLISECONDS);
        assertThat(subscriber.getOnNextEvents(), contains(Pair.of(42, ids)));
    }

    @Test
    public void process_entity_updates_and_send_results() throws Exception {
        final List<EntityId> ids = newArrayList(mock(EntityId.class));
        final Object data = mock(Object.class);
        final List<Feature> features = newArrayList(mock(Feature.class), mock(Feature.class));
        when(mux.call(any())).thenReturn(just(Pair.of(56, features)));
        when(generator.name()).thenReturn("foo");
        when(generator.generate(any())).thenReturn(data);


        final TestSubscriber<SocketMessage> subscriber = TestSubscriber.create();
        just(status(ids)).compose(handler()).subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        verify(generator).generate(features);
        assertThat(subscriber.getOnNextEvents(), contains(SelectionDrivenUpdateMessageImpl.of("foo", 56, data)));
    }

    @Test
    public void ignore_status_changes_not_involving_layer_subscription_change() throws Exception {
        final List<EntityId> ids = newArrayList(mock(EntityId.class));
        final ClientStatusMessage statusA = status(ids);
        final ClientStatusMessage statusB = status(ids);
        when(statusA.geofence()).thenReturn(mock(GeofenceStatus.class));
        when(statusB.geofence()).thenReturn(mock(GeofenceStatus.class));  // Different

        final TestSubscriber<Pair<Integer, List<EntityId>>> subscriber = subscriberFromMux();

        just(status(ids), status(ids))
                .compose(handler())
                .subscribe();

        subscriber.awaitTerminalEvent();
        assertThat(subscriber.getOnNextEvents(), hasSize(1));
    }

    private SelectionHandler handler() {
        return new SelectionHandler(singletonList(generator), mux);
    }

    private ClientStatusMessage status(List<EntityId> ids) {
        final ClientStatusMessage msg = mock(ClientStatusMessage.class);
        when(msg.selection()).thenReturn(SelectionStatusImpl.of(42, ids));
        return msg;
    }

    private TestSubscriber<Pair<Integer, List<EntityId>>> subscriberFromMux() {
        final TestSubscriber<Pair<Integer, List<EntityId>>> subscriber = TestSubscriber.create();
        when(mux.call(any())).then(invocation -> {
            final Observable<Pair<Integer, List<EntityId>>> observable = invocation.getArgument(0);
            observable.subscribe(subscriber);
            return empty();
        });
        return subscriber;
    }
}
