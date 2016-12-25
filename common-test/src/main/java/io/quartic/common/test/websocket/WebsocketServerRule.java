package io.quartic.common.test.websocket;

import org.glassfish.tyrus.server.TyrusServerContainer;
import org.glassfish.tyrus.spi.ServerContainer;
import org.junit.rules.ExternalResource;

import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.websocket.WebsocketUtilsKt.serverEndpointConfig;
import static java.util.Arrays.asList;
import static org.glassfish.tyrus.spi.ServerContainerFactory.createServerContainer;

public class WebsocketServerRule extends ExternalResource {
    private ServerContainer container;
    private int port;

    private final AtomicInteger numConnections = new AtomicInteger();
    private final List<String> messages = newArrayList();

    public class DummyEndpoint extends Endpoint {
        @Override
        public void onOpen(Session session, EndpointConfig config) {
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
        return "ws://localhost:" + port + "/ws";
    }

    @Override
    protected void before() throws Throwable {
        try {
            container = createServerContainer(null);
            container.addEndpoint(serverEndpointConfig("/ws", new DummyEndpoint()));
            container.start("", 0);
            port = ((TyrusServerContainer) container).getPort();
        } catch (IOException e) {
            throw new DeploymentException(e.getMessage(), e);
        }
    }

    @Override
    protected void after() {
        if (container != null) {
            container.stop();
            container = null;
            port = 0;
        }
    }

    public int numConnections() {
        return numConnections.get();
    }

    public void setMessages(String... messages) {
        setMessages(asList(messages));
    }

    public void setMessages(List<String> messages) {
        this.messages.clear();
        this.messages.addAll(messages);
    }
}
