package io.quartic.weyl.response;

import io.quartic.weyl.core.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractPong {
    String version();
}
