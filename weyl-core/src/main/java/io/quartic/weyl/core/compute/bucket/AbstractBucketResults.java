package io.quartic.weyl.core.compute.bucket;

import io.quartic.weyl.core.SweetStyle;
import io.quartic.weyl.core.model.AttributeSchema;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.LayerMetadata;
import org.immutables.value.Value;

import java.util.Collection;

@SweetStyle
@Value.Immutable
public interface AbstractBucketResults {
    LayerMetadata metadata();
    Collection<Feature> features();
    AttributeSchema schema();
}
