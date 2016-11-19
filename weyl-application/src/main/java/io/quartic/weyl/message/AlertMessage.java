package io.quartic.weyl.message;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.alert.Alert;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AlertMessage extends SocketMessage {
    @JsonUnwrapped
    Alert alert();
}
