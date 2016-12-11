package io.quartic.weyl.core;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.live.LayerView;
import io.quartic.weyl.core.model.AttributeSchema;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import org.immutables.value.Value;
import rx.Observable;

@SweetStyle
@Value.Immutable
public interface LayerSpec {
    LayerId id();
    LayerMetadata metadata();
    LayerView view();
    AttributeSchema schema();
    boolean indexable();
    @Value.Auxiliary    // No point having this contribute to equals(), etc.
    Observable<LayerUpdate> updates();
}
