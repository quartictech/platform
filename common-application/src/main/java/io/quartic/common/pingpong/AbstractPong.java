package io.quartic.common.pingpong;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractPong {
    String version();
}
