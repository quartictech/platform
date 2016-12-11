package io.quartic.weyl.core.source;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.live.LayerView;
import io.quartic.weyl.core.model.AttributeSchema;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import org.immutables.value.Value;
import rx.Observable;

@SweetStyle
@Value.Immutable
public interface SourceDescriptor {
    LayerId id();
    LayerMetadata metadata();
    LayerView view();
    AttributeSchema schema();
    boolean indexable();
    Observable<SourceUpdate> updates();
}
