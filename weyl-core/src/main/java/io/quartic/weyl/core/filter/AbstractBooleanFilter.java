package io.quartic.weyl.core.filter;

import io.quartic.weyl.core.geojson.SweetStyle;
import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

import java.util.Collection;

@SweetStyle
@Value.Immutable
public abstract class AbstractBooleanFilter implements Filter {
    abstract BooleanOperator operator();
    abstract Collection<Filter> filters();

    @Override
    public boolean apply(Feature feature) {
        switch (operator()) {
            case AND:
                return filters().stream().allMatch(filter -> filter.apply(feature));
            case OR:
                return filters().stream().anyMatch(filter -> filter.apply(feature));
            case NOR:
                return filters().stream().noneMatch(filter -> filter.apply(feature));
            default:
                throw new IllegalArgumentException("unrecognised operator " + operator());
        }
    }
}
