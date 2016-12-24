package io.quartic.weyl.update;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.quartic.weyl.websocket.ClientStatusMessageHandler;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.PingMessage;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.slf4j.Logger;
import rx.Observable;
import rx.Subscription;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler.Whole;
import javax.websocket.Session;
import java.io.IOException;
import java.util.Collection;

import static io.quartic.common.rx.RxUtils.combine;
import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static io.quartic.common.uid.UidUtils.stringify;
import static org.slf4j.LoggerFactory.getLogger;
import static rx.Emitter.BackpressureMode.BUFFER;
import static rx.Observable.fromEmitter;
import static rx.Observable.merge;

@Metered
@Timed
@ExceptionMetered
public class UpdateServer extends Endpoint {
    private static final Logger LOG = getLogger(UpdateServer.class);
    private static final String SUBSCRIPTION = "subscription";
    private final Observable<? extends SocketMessage> messages;
    private final Collection<ClientStatusMessageHandler> handlers;

    public UpdateServer(
            Observable<? extends SocketMessage> messages,
            Collection<ClientStatusMessageHandler> handlers
    ) {
        this.messages = messages;
        this.handlers = handlers;
    }

    @Override
    public void onOpen(final Session session, EndpointConfig config) {
        LOG.info("[{}] Open", session.getId());
        final Subscription subscription = merge(
                receivedMessages(session).compose(combine(handlers)),
                messages
        ).subscribe((message) -> sendMessage(session, message));
        session.getUserProperties().put(SUBSCRIPTION, subscription);
    }

    private Observable<ClientStatusMessage> receivedMessages(Session session) {
        return fromEmitter(emitter -> {
            // See https://github.com/eclipse/jetty.project/issues/207
            // noinspection Convert2Lambda,Anonymous2MethodRef
            session.addMessageHandler(new Whole<String>() {
                @Override
                public void onMessage(String message) {
                    try {
                        final SocketMessage msg = OBJECT_MAPPER.readValue(message, SocketMessage.class);
                        if (msg instanceof ClientStatusMessage) {
                            ClientStatusMessage csm = (ClientStatusMessage)msg;
                            LOG.info("[{}] Subscribed to layers {} + entities {}",
                                    session.getId(), stringify(csm.openLayerIds()), stringify(csm.selection().entityIds()));
                            emitter.onNext(csm);
                        } else if (msg instanceof PingMessage) {
                            LOG.info("[{}] Received ping", session.getId());
                        } else {
                            throw new RuntimeException("Unrecognised type '" + msg.getClass().getCanonicalName() + "'");
                        }
                    } catch (IOException e) {
                        LOG.warn("Error handling message", e);
                    }
                }
            });
        }, BUFFER);
    }

    private void sendMessage(Session session, SocketMessage message) {
        try {
            session.getAsyncRemote().sendText(OBJECT_MAPPER.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            LOG.error("Error producing JSON", e);
        }
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        LOG.info("[{}] Close", session.getId());
        final Subscription subscription = (Subscription) session.getUserProperties().get(SUBSCRIPTION);
        subscription.unsubscribe();
    }
}
