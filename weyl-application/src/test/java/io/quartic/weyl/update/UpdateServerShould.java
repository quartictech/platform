package io.quartic.weyl.update;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quartic.common.test.rx.ObservableInterceptor;
import io.quartic.weyl.core.alert.Alert;
import io.quartic.weyl.core.alert.AlertImpl;
import io.quartic.weyl.core.geofence.GeofenceType;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.websocket.ClientStatusMessageHandler;
import io.quartic.weyl.websocket.message.AlertMessageImpl;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.ClientStatusMessageImpl;
import io.quartic.weyl.websocket.message.GeofenceStatusImpl;
import io.quartic.weyl.websocket.message.SelectionStatusImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.serdes.ObjectMappers.encode;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

public class UpdateServerShould {
    private final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
    private final PublishSubject<Alert> alerts = PublishSubject.create();
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

        final UpdateServer server = createAndOpenServer(alerts);
        server.onMessage(encode(msg));

        subscriber.awaitValueCount(1, 100, MILLISECONDS);
        assertThat(subscriber.getOnNextEvents().get(0), equalTo(msg));
        verifyMessage(response);
    }

    @Test
    public void remove_listener_and_close_handlers_on_close() throws Exception {
        final ObservableInterceptor<SocketMessage> messageInterceptor = ObservableInterceptor.create();
        final ObservableInterceptor<Alert> alertInterceptor = ObservableInterceptor.create(alerts);
        when(handler.call(any())).thenReturn(messageInterceptor.observable());

        final UpdateServer server = createAndOpenServer(alertInterceptor.observable());
        server.onClose(session, mock(CloseReason.class));

        assertThat(alertInterceptor.unsubscribed(), equalTo(true));
        assertThat(messageInterceptor.unsubscribed(), equalTo(true));
    }

    @Test
    public void send_alert() throws Exception {
        final Alert alert = AlertImpl.of("foo", Optional.of("bar"), Alert.Level.SEVERE);

        final UpdateServer server = createAndOpenServer(alerts);
        alerts.onNext(alert);

        verifyMessage(AlertMessageImpl.of(alert));
    }

    private ClientStatusMessage clientStatusMessage() {
        return ClientStatusMessageImpl.of(
                emptyList(),
                SelectionStatusImpl.of(42, emptyList()),
                GeofenceStatusImpl.of(true, GeofenceType.EXCLUDE, Optional.empty(), Optional.empty(), 0.0)
        );
    }

    private void verifyMessage(Object expected) throws JsonProcessingException {
        verify(session.getAsyncRemote()).sendText(encode(expected));
    }

    private UpdateServer createAndOpenServer(Observable<Alert> alerts) {
        final UpdateServer server = createServer(alerts);
        server.onOpen(session, mock(EndpointConfig.class));
        return server;
    }

    private UpdateServer createServer(Observable<Alert> alerts) {
        return new UpdateServer(alerts, newArrayList(handler));
    }
}
