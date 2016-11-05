package io.quartic.terminator;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
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
public class SocketServer implements Subscriber<LiveEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(SocketServer.class);

    private final

    private Session session;


    @OnOpen
    public void onOpen(final Session session, EndpointConfig config) {
        this.session = session;
        LOG.info("[{}] Open", session.getId());
    }



    public void doStuff() {

    }
}
