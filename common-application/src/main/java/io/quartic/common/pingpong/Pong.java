package io.quartic.common.pingpong;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = PongImpl.class)
@JsonDeserialize(as = PongImpl.class)
public interface Pong {
    String version();
}
