package io.quartic.weyl.core.v2;

import io.quartic.weyl.core.SweetStyle;
import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

import java.util.Collection;

@SweetStyle
@Value.Immutable
public interface AbstractLayerAppendEvent extends LayerEvent {
   Collection<Feature> features();

    @Override
    default <T> T accept(LayerEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
