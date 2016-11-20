package io.quartic.weyl.core.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.live.LayerViewType;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = MapDatasetExtensionImpl.class)
@JsonDeserialize(as = MapDatasetExtensionImpl.class)
public interface MapDatasetExtension {
    @Value.Default
    default LayerViewType viewType() {
        return LayerViewType.MOST_RECENT;
    }
    Optional<AttributeName> titleAttribute();
    Optional<AttributeName> imageAttribute();
    List<AttributeName> blessedAttributes();
}
