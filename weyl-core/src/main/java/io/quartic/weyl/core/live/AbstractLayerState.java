package io.quartic.weyl.core.live;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.AbstractAttributeSchema;
import io.quartic.weyl.core.model.AbstractFeature;
import org.immutables.value.Value;

import java.util.Collection;

@Value.Immutable
@SweetStyle
public interface AbstractLayerState {
    AbstractAttributeSchema schema();
    Collection<AbstractFeature> featureCollection();
}
