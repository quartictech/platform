package io.quartic.weyl.core.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.live.LayerViewType;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = MapDatasetExtensionImpl.class)
@JsonDeserialize(as = MapDatasetExtensionImpl.class)
public interface MapDatasetExtension {
    @Value.Default
    default LayerViewType viewType() {
        return LayerViewType.MOST_RECENT;
    }
    @JsonUnwrapped
    StaticSchema staticSchema();
}
