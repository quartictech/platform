package io.quartic.weyl.core.compute;

import io.quartic.weyl.core.SweetStyle;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractBucketSpec extends ComputationSpec {
    LayerId buckets();
    LayerId features();
    BucketAggregation aggregation();
    boolean normalizeToArea();
}
