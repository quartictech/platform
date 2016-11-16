package io.quartic.weyl.core.compute;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.AttributeSchema;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.LayerMetadata;
import org.immutables.value.Value;

import java.util.Collection;

@SweetStyle
@Value.Immutable
public interface AbstractComputationResults {
    LayerMetadata metadata();
    Collection<AbstractFeature> features();
    AttributeSchema schema();
}
