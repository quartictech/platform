package io.quartic.weyl.core.compute;

import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

@Value.Immutable
public interface BucketSpec {
    String layerName();
    String aggregationPropertyName();
    LayerId buckets();
    LayerId features();
    BucketAggregation aggregation();
}
