package io.quartic.weyl.core.compute;

import io.quartic.weyl.common.SweetStyle;
import io.quartic.weyl.core.model.AttributeName;
import org.immutables.value.Value;

import java.util.Collection;

@SweetStyle
@Value.Immutable
public interface AbstractHistogram {
    @SweetStyle
    @Value.Immutable
    interface AbstractBucket {
        Object value();
        Long count();
    }

    AttributeName attribute();
    Collection<Bucket> buckets();
}
