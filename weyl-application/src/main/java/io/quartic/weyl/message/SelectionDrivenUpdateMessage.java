package io.quartic.weyl.message;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = SelectionDrivenUpdateMessageImpl.class)
@JsonDeserialize(as = SelectionDrivenUpdateMessageImpl.class)
public interface SelectionDrivenUpdateMessage extends SocketMessage {
    String subscriptionName();
    int seqNum();
    Object data();
}
