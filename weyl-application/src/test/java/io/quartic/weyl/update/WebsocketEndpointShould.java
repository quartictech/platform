package io.quartic.weyl.update;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quartic.common.test.rx.Interceptor;
import io.quartic.weyl.core.geofence.GeofenceType;
import io.quartic.weyl.core.model.Alert;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.websocket.ClientStatusMessageHandler;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.ClientStatusMessage.GeofenceStatus;
import io.quartic.weyl.websocket.message.ClientStatusMessage.SelectionStatus;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import rx.Observable;
import rx.observers.TestSubscriber;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler.Whole;
import javax.websocket.Session;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static io.quartic.common.serdes.ObjectMappersKt.encode;
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

public class WebsocketEndpointShould {
    private final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
    private final ClientStatusMessageHandler handler = mock(ClientStatusMessageHandler.class);

    @Before
    public void before() throws Exception {
        final Map<String, Object> map = newHashMap();
        when(session.getUserProperties()).thenReturn(map);
    }

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

        createAndOpenEndpoint();
        captureMessageHandler().onMessage(encode(msg));

        subscriber.awaitValueCount(1, 100, MILLISECONDS);
        assertThat(subscriber.getOnNextEvents().get(0), equalTo(msg));
        verifyMessage(response);
    }

    @Test
    public void send_message() throws Exception {
        final SocketMessage message = new SocketMessage() {};

        createAndOpenEndpoint(just(message));

        verifyMessage(message);
    }

    @Test
    public void remove_listener_and_close_handlers_on_close() throws Exception {
        final Interceptor<SocketMessage> handlerMessageInterceptor = new Interceptor<>();
        final Interceptor<SocketMessage> messageInterceptor = new Interceptor<>();
        when(handler.call(any())).thenReturn(Observable.<SocketMessage>never().compose(handlerMessageInterceptor));

        final WebsocketEndpoint endpoint = createAndOpenEndpoint(Observable.<SocketMessage>never().compose(messageInterceptor));
        endpoint.onClose(session, mock(CloseReason.class));

        assertThat(messageInterceptor.getUnsubscribed(), equalTo(true));
        assertThat(handlerMessageInterceptor.getUnsubscribed(), equalTo(true));
    }

    private ClientStatusMessage clientStatusMessage() {
        // It would be nice to mock all of this, but we need to serialise
        return new ClientStatusMessage(
                emptyList(),
                new SelectionStatus(42, emptyList()),
                new GeofenceStatus(
                        GeofenceType.EXCLUDE,
                        Alert.Level.WARNING,
                        null,
                        null,
                        0.0
                )
        );
    }

    private void verifyMessage(Object expected) throws JsonProcessingException {
        verify(session.getAsyncRemote()).sendText(encode(expected));
    }

    private WebsocketEndpoint createAndOpenEndpoint() {
        return createAndOpenEndpoint(never());
    }

    private WebsocketEndpoint createAndOpenEndpoint(Observable<SocketMessage> messages) {
        final WebsocketEndpoint endpoint = new WebsocketEndpoint(messages, newArrayList(handler));
        endpoint.onOpen(session, mock(EndpointConfig.class));
        return endpoint;
    }

    private Whole<String> captureMessageHandler() throws Exception {
        @SuppressWarnings("unchecked") ArgumentCaptor<Whole<String>> captor = ArgumentCaptor.forClass(Whole.class);
        verify(session).addMessageHandler(captor.capture());
        return captor.getValue();
    }
}
