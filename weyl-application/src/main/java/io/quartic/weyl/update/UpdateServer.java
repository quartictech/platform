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
import rx.subjects.PublishSubject;

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
import static rx.Observable.merge;

@Metered
@Timed
@ExceptionMetered
public class UpdateServer extends Endpoint {
    private static final Logger LOG = getLogger(UpdateServer.class);
    private final PublishSubject<ClientStatusMessage> clientStatus = PublishSubject.create();
    private final Observable<? extends SocketMessage> messages;
    private final Collection<ClientStatusMessageHandler> handlers;
    private Subscription subscription;
    private Session session;

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
        this.session = session;
        this.subscription = merge(
                clientStatus.compose(combine(handlers)),
                messages
        ).subscribe(this::sendMessage);
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
                                UpdateServer.this.session.getId(), stringify(csm.openLayerIds()), stringify(csm.selection().entityIds()));
                        clientStatus.onNext(csm);
                    } else if (msg instanceof PingMessage) {
                        LOG.info("[{}] Received ping", UpdateServer.this.session.getId());
                    } else {
                        throw new RuntimeException("Unrecognised type '" + msg.getClass().getCanonicalName() + "'");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        LOG.info("[{}] Close", session.getId());
        subscription.unsubscribe();
    }

    private void sendMessage(SocketMessage message) {
        try {
            session.getAsyncRemote().sendText(OBJECT_MAPPER.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            LOG.error("Error producing JSON", e);
        }
    }
}
