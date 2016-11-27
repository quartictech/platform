package io.quartic.weyl.websocket;

import io.quartic.weyl.websocket.message.ClientStatusMessage;
import io.quartic.weyl.websocket.message.SocketMessage;
import rx.Observable;

public interface ClientStatusMessageHandler extends Observable.Transformer<ClientStatusMessage, SocketMessage> {
}
