package io.quartic.terminator;

import com.google.common.collect.ImmutableList;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;

public class CollectingEndpoint<T> extends Endpoint {
    private final Class<? extends T> clazz;
    private final List<T> messages = newArrayList();

    public CollectingEndpoint(Class<? extends T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        // Suppress conversion because this somehow borks the websocket internals - "unable to find decoder"
        //noinspection Convert2Lambda
        session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                try {
                    messages.add(OBJECT_MAPPER.readValue(message, clazz));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public boolean awaitMessages(int expected, long timeout, TimeUnit unit) {
        while (timeout != 0 && messages.size() < expected) {
            try {
                unit.sleep(1);
            } catch (InterruptedException e) {
                throw new IllegalStateException("Interrupted", e);
            }
            timeout--;
        }
        return messages.size() >= expected;
    }

    public List<T> messages() {
        return ImmutableList.copyOf(messages);
    }
}
