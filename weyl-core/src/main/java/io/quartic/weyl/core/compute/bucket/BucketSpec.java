package io.quartic.weyl.core.compute.bucket;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.weyl.core.compute.ImmutableBucketSpec;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableBucketSpec.class)
@JsonDeserialize(as = ImmutableBucketSpec.class)
public interface BucketSpec {
    LayerId buckets();
    LayerId features();
    BucketAggregation aggregation();
    boolean normalizeToArea();
}
