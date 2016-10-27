package io.quartic.common.pingpong;

import io.quartic.weyl.core.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractPong {
    String version();
}
