package io.quartic.weyl.core.filter;

import io.quartic.weyl.core.geojson.SweetStyle;
import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

import java.util.Set;

@SweetStyle
@Value.Immutable
public abstract class AbstractSetPropertyValueFilter implements Filter {
    abstract String property();
    abstract SetPropertyValueOperator operator();
    abstract Set<Object> values();
    abstract boolean stringifyValues();

    private Object matchValue(Feature feature) {
        Object value = feature.metadata().get(property());
        if (value == null) {
            return null;
        }
        return stringifyValues() ? value.toString() : value;
    }

    @Override
    public boolean apply(Feature input) {
        Object value = matchValue(input);
        switch (operator()) {
            case IN:
                return values().contains(value);
            case NOT_IN:
                return !values().contains(value);
            default:
                throw new IllegalArgumentException("operator not recognised: " + operator());
        }
    }
}
