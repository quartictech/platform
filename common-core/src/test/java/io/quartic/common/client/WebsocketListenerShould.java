package io.quartic.common.client;

import io.quartic.common.websocket.WebsocketServerRule;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import rx.observers.TestSubscriber;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class WebsocketListenerShould {
    private static final int TIMEOUT_MILLISECONDS = 250;

    @Rule
    public WebsocketServerRule server = new WebsocketServerRule();

    @Test
    public void emit_items_from_socket() throws Exception {
        server.setMessages("foo", "bar");

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

        assertThat(server.numConnections(), Matchers.equalTo(1));
    }

    @Test
    public void not_connect_to_websocket_if_no_subscribers() throws Exception {
        createListener();

        assertThat(server.numConnections(), Matchers.equalTo(0));
    }

    private WebsocketListener createListener() {
        return WebsocketListener.builder()
                .websocketFactory(new WebsocketClientSessionFactory(getClass()))
                .name("Budgie")
                .url(server.uri())
                .build();
    }
}
