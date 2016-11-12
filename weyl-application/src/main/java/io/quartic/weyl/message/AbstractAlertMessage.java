package io.quartic.weyl.message;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.quartic.weyl.core.alert.AbstractAlert;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractAlertMessage extends SocketMessage {
    @JsonUnwrapped
    AbstractAlert alert();
}
