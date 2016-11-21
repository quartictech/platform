package io.quartic.weyl.core.compute;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = BucketSpecImpl.class)
@JsonDeserialize(as = BucketSpecImpl.class)
public interface BucketSpec extends ComputationSpec {
    LayerId buckets();
    LayerId features();
    BucketAggregation aggregation();
    boolean normalizeToArea();
}
