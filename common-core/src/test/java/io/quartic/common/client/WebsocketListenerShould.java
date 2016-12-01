package io.quartic.common.client;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.common.test.websocket.WebsocketServerRule;
import org.hamcrest.Matchers;
import org.immutables.value.Value;
import org.junit.Rule;
import org.junit.Test;
import rx.observers.TestSubscriber;

import static io.quartic.common.serdes.ObjectMappers.encode;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class WebsocketListenerShould {
    private static final int TIMEOUT_MILLISECONDS = 1000;

    @Rule
    public WebsocketServerRule server = new WebsocketServerRule();

    @Test
    public void emit_items_from_socket() throws Exception {
        server.setMessages(encode(TestThingImpl.of("foo")), encode(TestThingImpl.of("bar")));

        final WebsocketListener<TestThing> listener = createListener();

        TestSubscriber<TestThing> subscriber = TestSubscriber.create();
        listener.observable().subscribe(subscriber);
        subscriber.awaitValueCount(2, TIMEOUT_MILLISECONDS, MILLISECONDS);

        assertThat(subscriber.getOnNextEvents(), contains(TestThingImpl.of("foo"), TestThingImpl.of("bar")));
    }

    @Test
    public void skip_undecodable_items() throws Exception {
        server.setMessages("bad", encode(TestThingImpl.of("bar")));

        final WebsocketListener<TestThing> listener = createListener();

        TestSubscriber<TestThing> subscriber = TestSubscriber.create();
        listener.observable().subscribe(subscriber);
        subscriber.awaitValueCount(1, TIMEOUT_MILLISECONDS, MILLISECONDS);

        assertThat(subscriber.getOnNextEvents(), contains(TestThingImpl.of("bar")));
    }

    @Test
    public void only_create_one_connection_if_multiple_subscribers() throws Exception {
        final WebsocketListener<TestThing> listener = createListener();

        TestSubscriber<TestThing> subA = TestSubscriber.create();
        TestSubscriber<TestThing> subB = TestSubscriber.create();
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

    private WebsocketListener<TestThing> createListener() {
        return WebsocketListener.Factory.of(server.uri(), new WebsocketClientSessionFactory(getClass()))
                .create(TestThing.class);
    }

    @SweetStyle
    @Value.Immutable
    @JsonSerialize(as = TestThingImpl.class)
    @JsonDeserialize(as = TestThingImpl.class)
    interface TestThing {
        String name();
    }
}
