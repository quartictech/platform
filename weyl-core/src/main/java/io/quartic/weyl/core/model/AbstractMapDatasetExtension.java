package io.quartic.weyl.core.model;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.live.LayerViewType;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractMapDatasetExtension {
    @Value.Default
    default LayerViewType viewType() {
        return LayerViewType.MOST_RECENT;
    }
}
