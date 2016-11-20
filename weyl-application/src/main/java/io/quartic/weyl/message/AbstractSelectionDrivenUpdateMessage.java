package io.quartic.weyl.message;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractSelectionDrivenUpdateMessage extends SocketMessage {
    String subscriptionName();
    int seqNum();
    Object data();
}
