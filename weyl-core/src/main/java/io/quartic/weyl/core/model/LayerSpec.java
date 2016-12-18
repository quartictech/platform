package io.quartic.weyl.core.model;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.live.LayerView;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface LayerSpec {
    LayerId id();
    LayerMetadata metadata();
    LayerView view();
    StaticSchema staticSchema();
    boolean indexable();
}
