package io.quartic.weyl.update;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.quartic.weyl.core.alert.Alert;
import io.quartic.weyl.core.alert.AlertListener;
import io.quartic.weyl.core.alert.AlertProcessor;
import io.quartic.weyl.websocket.ClientStatusMessageHandler;
import io.quartic.weyl.websocket.message.AlertMessageImpl;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.PingMessage;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.slf4j.Logger;
import rx.Subscription;
import rx.subjects.PublishSubject;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collection;

import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static io.quartic.common.uid.UidUtils.stringify;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;
import static rx.Observable.merge;

@Metered
@Timed
@ExceptionMetered
@ServerEndpoint("/ws")
public class UpdateServer implements AlertListener {
    private static final Logger LOG = getLogger(UpdateServer.class);
    private final PublishSubject<ClientStatusMessage> clientStatus = PublishSubject.create();
    private final AlertProcessor alertProcessor;
    private final Collection<ClientStatusMessageHandler> handlers;
    private Subscription subscription;
    private Session session;

    public UpdateServer(
            AlertProcessor alertProcessor,
            Collection<ClientStatusMessageHandler> handlers
    ) {
        this.alertProcessor = alertProcessor;
        this.handlers = handlers;
    }

    @OnOpen
    public void onOpen(final Session session, EndpointConfig config) {
        LOG.info("[{}] Open", session.getId());
        this.session = session;
        alertProcessor.addListener(this);
        this.subscription = merge(
                handlers.stream()
                        .map(factory -> clientStatus.compose(factory))
                        .collect(toList())
        ).subscribe(msg -> sendMessage(msg));
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            final SocketMessage msg = OBJECT_MAPPER.readValue(message, SocketMessage.class);
            if (msg instanceof ClientStatusMessage) {
                ClientStatusMessage csm = (ClientStatusMessage)msg;
                LOG.info("[{}] Subscribed to layers {} + entities {}",
                        session.getId(), stringify(csm.subscribedLiveLayerIds()), stringify(csm.selection().entityIds()));
                clientStatus.onNext(csm);
            } else if (msg instanceof PingMessage) {
                LOG.info("[{}] Received ping", session.getId());
            } else {
                throw new RuntimeException("Unrecognised type '" + msg.getClass().getCanonicalName() + "'");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        LOG.info("[{}] Close", session.getId());
        alertProcessor.removeListener(this);
        subscription.unsubscribe();
    }

    @Override
    public void onAlert(Alert alert) {
        sendMessage(AlertMessageImpl.of(alert));
    }

    private void sendMessage(SocketMessage message) {
        try {
            session.getAsyncRemote().sendText(OBJECT_MAPPER.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            LOG.error("Error producing JSON", e);
        }
    }
}
