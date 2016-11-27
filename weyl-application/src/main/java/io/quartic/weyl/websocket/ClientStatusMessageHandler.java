package io.quartic.weyl.websocket;

import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.SocketMessage;

import java.util.function.Consumer;

public interface ClientStatusMessageHandler extends AutoCloseable {
    interface Factory {
        ClientStatusMessageHandler create(Consumer<SocketMessage> messageConsumer);
    }

    void onClientStatusMessage(ClientStatusMessage msg);
}
