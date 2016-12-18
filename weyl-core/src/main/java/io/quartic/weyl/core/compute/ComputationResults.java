package io.quartic.weyl.core.compute;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.DynamicSchema;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.model.NakedFeature;
import org.immutables.value.Value;

import java.util.Collection;

@SweetStyle
@Value.Immutable
public interface ComputationResults {
    LayerMetadata metadata();
    DynamicSchema schema();
    Collection<NakedFeature> features();
}
