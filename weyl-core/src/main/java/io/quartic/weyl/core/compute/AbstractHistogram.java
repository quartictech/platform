package io.quartic.weyl.core.compute;

import io.quartic.weyl.core.geojson.SweetStyle;
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

    String property();
    Collection<Bucket> buckets();
}
