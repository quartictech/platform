package io.quartic.weyl.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quartic.weyl.core.alert.Alert;
import io.quartic.weyl.core.alert.AlertImpl;
import io.quartic.weyl.core.alert.AlertProcessor;
import io.quartic.weyl.core.geofence.GeofenceType;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.websocket.message.*;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.serdes.ObjectMappers.encode;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static rx.Observable.empty;
import static rx.Observable.just;

public class UpdateServerShould {
    private final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
    private final AlertProcessor alertProcessor = mock(AlertProcessor.class);
    private final ClientStatusMessageHandler handler = mock(ClientStatusMessageHandler.class);

    @Test
    public void compose_received_messages_via_handlers() throws Exception {
        final ClientStatusMessage msg = clientStatusMessage();
        final SocketMessage response = new SocketMessage() {};

        final TestSubscriber<Pair<Integer, List<EntityId>>> subscriber = TestSubscriber.create();
        when(handler.call(any())).then(invocation -> {
            final Observable<Pair<Integer, List<EntityId>>> observable = invocation.getArgument(0);
            observable.subscribe(subscriber);
            return just(response);
        });

        final UpdateServer server = createAndOpenServer();
        server.onMessage(encode(msg));

        subscriber.awaitValueCount(1, 100, MILLISECONDS);
        assertThat(subscriber.getOnNextEvents().get(0), equalTo(msg));
        verifyMessage(response);
    }

    @Test
    public void add_listener_on_open() {
        final UpdateServer server = createAndOpenServer();

        verify(alertProcessor).addListener(server);
    }

    @Test
    public void remove_listener_and_close_handlers_on_close() throws Exception {
        final AtomicBoolean unsubscribed = new AtomicBoolean(false);
        final Observable<SocketMessage> observable = empty();
        when(handler.call(any())).thenReturn(observable.doOnUnsubscribe(() -> unsubscribed.set(true)));

        final UpdateServer server = createAndOpenServer();
        server.onClose(session, mock(CloseReason.class));

        verify(alertProcessor).removeListener(server);
        assertThat(unsubscribed.get(), equalTo(true));
    }

    @Test
    public void send_alert() throws Exception {
        final Alert alert = AlertImpl.of("foo", "bar");

        final UpdateServer server = createAndOpenServer();
        server.onAlert(alert);

        verifyMessage(AlertMessageImpl.of(alert));
    }

    private ClientStatusMessage clientStatusMessage() {
        return ClientStatusMessageImpl.of(
                emptyList(),
                SelectionStatusImpl.of(42, emptyList()),
                GeofenceStatusImpl.of(GeofenceType.EXCLUDE, Optional.empty(), Optional.empty(), 0.0)
        );
    }

    private void verifyMessage(Object expected) throws JsonProcessingException {
        verify(session.getAsyncRemote()).sendText(encode(expected));
    }

    private UpdateServer createAndOpenServer() {
        final UpdateServer server = createServer();
        server.onOpen(session, mock(EndpointConfig.class));
        return server;
    }

    private UpdateServer createServer() {
        return new UpdateServer(
                alertProcessor,
                newArrayList(handler));
    }
}
