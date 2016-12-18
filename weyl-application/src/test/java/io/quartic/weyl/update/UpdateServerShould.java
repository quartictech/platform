package io.quartic.weyl.update;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quartic.common.test.rx.Interceptor;
import io.quartic.weyl.core.alert.Alert;
import io.quartic.weyl.core.geofence.GeofenceType;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.websocket.ClientStatusMessageHandler;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.ClientStatusMessageImpl;
import io.quartic.weyl.websocket.message.GeofenceStatusImpl;
import io.quartic.weyl.websocket.message.SelectionStatusImpl;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

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
import static rx.Observable.never;

public class UpdateServerShould {
    private final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
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
    public void send_message() throws Exception {
        final SocketMessage message = new SocketMessage() {};

        createAndOpenServer(just(message));

        verifyMessage(message);
    }

    @Test
    public void remove_listener_and_close_handlers_on_close() throws Exception {
        final Interceptor<SocketMessage> handlerMessageInterceptor = Interceptor.create();
        final Interceptor<SocketMessage> messageInterceptor = Interceptor.create();
        when(handler.call(any())).thenReturn(Observable.<SocketMessage>never().compose(handlerMessageInterceptor));

        final UpdateServer server = createAndOpenServer(Observable.<SocketMessage>never().compose(messageInterceptor));
        server.onClose(session, mock(CloseReason.class));

        assertThat(messageInterceptor.unsubscribed(), equalTo(true));
        assertThat(handlerMessageInterceptor.unsubscribed(), equalTo(true));
    }

    private ClientStatusMessage clientStatusMessage() {
        // It would be nice to mock all of this, but we need to serialise
        return ClientStatusMessageImpl.of(
                emptyList(),
                SelectionStatusImpl.of(42, emptyList()),
                GeofenceStatusImpl.of(
                        true,
                        GeofenceType.EXCLUDE,
                        Alert.Level.WARNING,
                        Optional.empty(),
                        Optional.empty(),
                        0.0
                )
        );
    }

    private void verifyMessage(Object expected) throws JsonProcessingException {
        verify(session.getAsyncRemote()).sendText(encode(expected));
    }

    private UpdateServer createAndOpenServer() {
        return createAndOpenServer(never());
    }

    private UpdateServer createAndOpenServer(Observable<SocketMessage> messages) {
        final UpdateServer server = new UpdateServer(messages, newArrayList(handler));
        server.onOpen(session, mock(EndpointConfig.class));
        return server;
    }

}
