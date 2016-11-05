package io.quartic.terminator;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.model.LiveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Subscriber;

import javax.websocket.EndpointConfig;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@Metered
@Timed
@ExceptionMetered
@ServerEndpoint("/ws")
public class SocketServer {
    private static final Logger LOG = LoggerFactory.getLogger(SocketServer.class);

    private Session session;

    @OnOpen
    public void onOpen(final Session session, EndpointConfig config) {
        this.session = session;
        LOG.info("[{}] Open", session.getId());
    }

    public Subscriber<LiveEvent> createEventSubscriber(DatasetId id) {
        return new Subscriber<LiveEvent>() {
            @Override
            public void onCompleted() {
                // TODO
                add();
            }

            @Override
            public void onError(Throwable e) {
                // TODO
            }

            @Override
            public void onNext(LiveEvent liveEvent) {
                // TODO
            }
        };
    }
}
