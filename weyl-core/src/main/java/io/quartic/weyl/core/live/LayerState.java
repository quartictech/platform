package io.quartic.weyl.core.live;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.AttributeSchema;
import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

import java.util.Collection;

@Value.Immutable
@SweetStyle
public interface LayerState {
    AttributeSchema schema();
    Collection<Feature> featureCollection();
}
