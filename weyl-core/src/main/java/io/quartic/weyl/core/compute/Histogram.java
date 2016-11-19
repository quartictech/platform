package io.quartic.weyl.core.compute;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.model.AttributeName;
import org.immutables.value.Value;

import java.util.Collection;

@SweetStyle
@Value.Immutable
public interface Histogram {
    @SweetStyle
    @Value.Immutable
    interface Bucket {
        Object value();
        Long count();
    }

    AttributeName attribute();
    Collection<Bucket> buckets();
}
