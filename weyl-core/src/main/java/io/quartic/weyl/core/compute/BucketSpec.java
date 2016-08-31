package io.quartic.weyl.core.compute;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableBucketSpec.class)
@JsonDeserialize(as = ImmutableBucketSpec.class)
public interface BucketSpec {
    String aggregationPropertyName();
    LayerId buckets();
    LayerId features();
    BucketAggregation aggregation();
}
