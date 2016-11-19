package io.quartic.weyl.core.model;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.live.LayerViewType;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@SweetStyle
@Value.Immutable
public interface MapDatasetExtension {
    @Value.Default
    default LayerViewType viewType() {
        return LayerViewType.MOST_RECENT;
    }
    Optional<AttributeName> titleAttribute();
    Optional<AttributeName> imageAttribute();
    List<AttributeName> blessedAttributes();
}
