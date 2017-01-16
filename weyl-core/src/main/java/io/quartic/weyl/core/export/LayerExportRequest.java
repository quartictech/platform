package io.quartic.weyl.core.export;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonDeserialize(as=LayerExportRequestImpl.class)
public interface LayerExportRequest {
    LayerId layerId();
}
