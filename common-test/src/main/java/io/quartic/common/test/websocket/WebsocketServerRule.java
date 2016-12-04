package io.quartic.common.test.websocket;

import com.google.common.collect.Lists;
import org.glassfish.tyrus.server.Server;
import org.junit.rules.ExternalResource;

import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;

public class WebsocketServerRule extends ExternalResource {
    private Server server;

    // TODO: This use of statics is utterly gross, but unclear how to avoid the static endpoint class
    private static final AtomicInteger numConnections = new AtomicInteger();
    private static final List<String> messages = Lists.newArrayList();

    @ServerEndpoint("/ws")
    public static class DummyEndpoint {
        @OnOpen
        public void onOpen(Session session) throws IOException {
            numConnections.incrementAndGet();
            messages.forEach(m -> {
                try {
                    session.getBasicRemote().sendText(m);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public String uri() {
        return "ws://localhost:" + server.getPort() + "/ws";
    }

    @Override
    protected void before() throws Throwable {
        numConnections.set(0);
        messages.clear();

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

    public void setMessages(String... messages) {
        setMessages(asList(messages));
    }

    public void setMessages(List<String> messages) {
        WebsocketServerRule.messages.clear();
        WebsocketServerRule.messages.addAll(messages);
    }
}
