package io.quartic.weyl.update;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.quartic.common.websocket.ResourceManagingEndpoint;
import io.quartic.weyl.websocket.ClientStatusMessageHandler;
import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.PingMessage;
import io.quartic.weyl.websocket.message.SocketMessage;
import org.slf4j.Logger;
import rx.Observable;
import rx.Subscription;

import javax.websocket.MessageHandler.Whole;
import javax.websocket.Session;
import java.io.IOException;
import java.util.Collection;

import static io.quartic.common.rx.RxUtilsKt.combine;
import static io.quartic.common.serdes.ObjectMappersKt.objectMapper;
import static org.slf4j.LoggerFactory.getLogger;
import static rx.Emitter.BackpressureMode.BUFFER;
import static rx.Observable.fromEmitter;

@Metered
@Timed
@ExceptionMetered
public class WebsocketEndpoint extends ResourceManagingEndpoint<Subscription> {
    private static final Logger LOG = getLogger(WebsocketEndpoint.class);
    private final Observable<? extends SocketMessage> messages;
    private final Collection<ClientStatusMessageHandler> handlers;

    public WebsocketEndpoint(
            Observable<? extends SocketMessage> messages,
            Collection<ClientStatusMessageHandler> handlers
    ) {
        this.messages = messages;
        this.handlers = handlers;
    }

    @Override
    protected Subscription createResourceFor(Session session) {
        return receivedMessages(session)
                .compose(combine(handlers))
                .mergeWith(messages)
                .subscribe(message -> sendMessage(session, message));
    }

    @Override
    protected void releaseResource(Subscription subscription) {
        subscription.unsubscribe();
    }

    private Observable<ClientStatusMessage> receivedMessages(Session session) {
        return fromEmitter(emitter -> {
            // See https://github.com/eclipse/jetty.project/issues/207
            // noinspection Convert2Lambda,Anonymous2MethodRef
            session.addMessageHandler(new Whole<String>() {
                @Override
                public void onMessage(String message) {
                    try {
                        final SocketMessage msg = objectMapper().readValue(message, SocketMessage.class);
                        if (msg instanceof ClientStatusMessage) {
                            ClientStatusMessage csm = (ClientStatusMessage)msg;
                            LOG.info("[{}] Subscribed to layers {} + entities {}",
                                    session.getId(), csm.openLayerIds(), csm.selection().entityIds());
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
            session.getAsyncRemote().sendText(objectMapper().writeValueAsString(message));
        } catch (JsonProcessingException e) {
            LOG.error("Error producing JSON", e);
        }
    }
}
