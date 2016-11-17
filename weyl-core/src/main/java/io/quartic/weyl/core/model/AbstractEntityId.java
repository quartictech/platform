package io.quartic.weyl.core.model;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractEntityId {
    LayerId layerId();
    String uid();   // TODO: not a string
}
