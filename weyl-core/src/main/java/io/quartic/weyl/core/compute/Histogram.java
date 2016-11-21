package io.quartic.weyl.core.compute;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.AttributeName;
import org.immutables.value.Value;

import java.util.Collection;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = HistogramImpl.class)
@JsonDeserialize(as = HistogramImpl.class)
public interface Histogram {
    @SweetStyle
    @Value.Immutable
    @JsonSerialize(as = BucketImpl.class)
    @JsonDeserialize(as = BucketImpl.class)
    interface Bucket {
        Object value();
        Long count();
    }

    AttributeName attribute();
    Collection<Bucket> buckets();
}
