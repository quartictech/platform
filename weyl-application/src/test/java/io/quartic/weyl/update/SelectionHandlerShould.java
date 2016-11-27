package io.quartic.weyl.update;

import io.quartic.weyl.Multiplexer;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.EntityIdImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.websocket.message.*;
import io.quartic.weyl.websocket.message.ClientStatusMessage.GeofenceStatus;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
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
        final ArrayList<EntityId> ids = newArrayList(EntityIdImpl.of("123"));
        final TestSubscriber<Pair<Integer, List<EntityId>>> subscriber = TestSubscriber.create();
        when(mux.call(any())).then(invocation -> {
            final Observable<Pair<Integer, List<EntityId>>> observable = invocation.getArgument(0);
            observable.subscribe(subscriber);
            return empty();
        });

        just(message(ids)).compose(handler()).subscribe();

        subscriber.awaitValueCount(1, 100, MILLISECONDS);
        assertThat(subscriber.getOnNextEvents(), contains(Pair.of(42, ids)));
    }

    @Test
    public void process_entity_updates_and_send_results() throws Exception {
        final ArrayList<EntityId> ids = newArrayList(EntityIdImpl.of("123"));
        final Object data = mock(Object.class);
        final List<Feature> features = newArrayList(mock(Feature.class), mock(Feature.class));
        when(mux.call(any())).thenReturn(just(Pair.of(56, features)));
        when(generator.name()).thenReturn("foo");
        when(generator.generate(any())).thenReturn(data);


        final TestSubscriber<SocketMessage> subscriber = TestSubscriber.create();
        just(message(ids)).compose(handler()).subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        verify(generator).generate(features);
        assertThat(subscriber.getOnNextEvents(), contains(SelectionDrivenUpdateMessageImpl.of("foo", 56, data)));
    }

    private SelectionHandler handler() {
        return new SelectionHandler(singletonList(generator), mux);
    }

    private ClientStatusMessage message(ArrayList<EntityId> ids) {
        return ClientStatusMessageImpl.of(
                emptyList(),
                SelectionStatusImpl.of(42, ids),
                mock(GeofenceStatus.class)
        );
    }
}
