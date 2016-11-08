package io.quartic.weyl.core.source;

import com.codahale.metrics.MetricRegistry;
import io.quartic.common.client.WebsocketClientSessionFactory;
import org.glassfish.tyrus.server.Server;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import rx.observers.TestSubscriber;

import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

public class WebsocketListenerShould {
    private static final int TIMEOUT_MILLISECONDS = 250;

    public static class WebsocketServerRule extends ExternalResource {
        private Server server;

        // TODO: This use of statics is utterly gross, but unclear how to avoid the static endpoint class
        private static final AtomicInteger numConnections = new AtomicInteger();

        @ServerEndpoint("/ws")
        public static class DummyEndpoint {
            @OnOpen
            public void onOpen(Session session) throws IOException {
                numConnections.incrementAndGet();
                session.getBasicRemote().sendText("foo");
                session.getBasicRemote().sendText("bar");
            }
        }

        public String uri() {
            return "ws://localhost:" + server.getPort() + "/ws";
        }

        @Override
        protected void before() throws Throwable {
            numConnections.set(0);
            server = new Server("localhost", -1, "", null, DummyEndpoint.class);
            server.start();
        }

        @Override
        protected void after() {
            server.stop();
        }

        public int numConnections() {
            return numConnections.get();
        }
    }

    @Rule
    public WebsocketServerRule server = new WebsocketServerRule();

    @Test
    public void emit_items_from_socket() throws Exception {
        final WebsocketListener listener = createListener();

        TestSubscriber<String> subscriber = TestSubscriber.create();
        listener.observable().subscribe(subscriber);
        subscriber.awaitValueCount(2, TIMEOUT_MILLISECONDS, MILLISECONDS);

        assertThat(subscriber.getOnNextEvents(), contains("foo", "bar"));
    }

    @Test
    public void only_create_one_connection_if_multiple_subscribers() throws Exception {
        final WebsocketListener listener = createListener();

        TestSubscriber<String> subA = TestSubscriber.create();
        TestSubscriber<String> subB = TestSubscriber.create();
        listener.observable().subscribe(subA);
        listener.observable().subscribe(subB);
        subA.awaitValueCount(1, TIMEOUT_MILLISECONDS, MILLISECONDS);
        subB.awaitValueCount(1, TIMEOUT_MILLISECONDS, MILLISECONDS);

        assertThat(server.numConnections(), equalTo(1));
    }

    @Test
    public void not_connect_to_websocket_if_no_subscribers() throws Exception {
        createListener();

        assertThat(server.numConnections(), equalTo(0));
    }

    private WebsocketListener createListener() {
        return WebsocketListener.builder()
                .websocketFactory(new WebsocketClientSessionFactory(getClass()))
                .name("Budgie")
                .url(server.uri())
                .metrics(mock(MetricRegistry.class, RETURNS_DEEP_STUBS))
                .build();
    }
}
