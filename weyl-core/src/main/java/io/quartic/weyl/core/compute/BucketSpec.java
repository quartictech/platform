package io.quartic.weyl.core.compute;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface BucketSpec extends ComputationSpec {
    LayerId buckets();
    LayerId features();
    BucketAggregation aggregation();
    boolean normalizeToArea();
}
