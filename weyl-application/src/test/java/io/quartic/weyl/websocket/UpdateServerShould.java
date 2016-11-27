package io.quartic.weyl.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quartic.weyl.core.alert.Alert;
import io.quartic.weyl.core.alert.AlertImpl;
import io.quartic.weyl.core.alert.AlertProcessor;
import io.quartic.weyl.core.geofence.GeofenceType;
import io.quartic.weyl.websocket.message.AlertMessageImpl;
import io.quartic.weyl.websocket.message.ClientStatusMessageImpl;
import io.quartic.weyl.websocket.message.GeofenceStatusImpl;
import io.quartic.weyl.websocket.message.SelectionStatusImpl;
import org.junit.Before;
import org.junit.Test;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.serdes.ObjectMappers.encode;
import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UpdateServerShould {
    private final Session session = mock(Session.class, RETURNS_DEEP_STUBS);
    private final AlertProcessor alertProcessor = mock(AlertProcessor.class);
    private final ClientStatusMessageHandler handler = mock(ClientStatusMessageHandler.class);
    private final ClientStatusMessageHandler.Factory handlerFactory = mock(ClientStatusMessageHandler.Factory.class);

    @Before
    public void before() throws Exception {
        when(handlerFactory.create(any())).thenReturn(handler);
    }

    @Test
    public void create_handlers_on_construction() throws Exception {
        createServer();

        verify(handlerFactory).create(any(Consumer.class));
    }

    @Test
    public void send_messages_to_handlers() throws Exception {
        final ClientStatusMessageImpl msg = ClientStatusMessageImpl.of(
                emptyList(),
                SelectionStatusImpl.of(42, emptyList()),
                GeofenceStatusImpl.of(GeofenceType.EXCLUDE, Optional.empty(), Optional.empty(), 0.0)
        );

        final UpdateServer server = createAndOpenServer();
        server.onMessage(encode(msg));

        verify(handler).onClientStatusMessage(msg);
    }

    @Test
    public void add_listener_on_open() {
        final UpdateServer server = createAndOpenServer();

        verify(alertProcessor).addListener(server);
    }

    @Test
    public void remove_listener_and_close_handlers_on_close() throws Exception {
        final UpdateServer server = createAndOpenServer();

        server.onClose(session, mock(CloseReason.class));

        verify(alertProcessor).removeListener(server);
        verify(handler).close();
    }

    @Test
    public void send_alert() throws Exception {
        final Alert alert = AlertImpl.of("foo", "bar");

        final UpdateServer server = createAndOpenServer();
        server.onAlert(alert);

        verifyMessage(AlertMessageImpl.of(alert));
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
                newArrayList(handlerFactory));
    }
}
