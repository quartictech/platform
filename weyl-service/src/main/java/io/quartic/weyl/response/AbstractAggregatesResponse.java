package io.quartic.weyl.response;

import com.google.common.collect.Multimap;
import io.quartic.weyl.core.geojson.SweetStyle;
import io.quartic.weyl.core.model.Uid;
import org.immutables.value.Value;

import java.util.Map;

@SweetStyle
@Value.Immutable
public interface AbstractAggregatesResponse {

    @SweetStyle
    @Value.Immutable
    interface AbstractValueId extends Uid {}

    @SweetStyle
    @Value.Immutable
    interface AbstractValueStats {
        Object value();
        Long count();
    }

    Map<ValueId, ValueStats> stats();
    Multimap<String, ValueId> propertyValues();
}
