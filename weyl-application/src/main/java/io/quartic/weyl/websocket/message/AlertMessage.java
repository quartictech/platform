package io.quartic.weyl.websocket.message;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.Alert;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = AlertMessageImpl.class)
@JsonDeserialize(as = AlertMessageImpl.class)
public interface AlertMessage extends SocketMessage {
    @JsonUnwrapped
    Alert alert();
}