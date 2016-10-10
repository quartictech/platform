package io.quartic.weyl.core.filter;

import io.quartic.weyl.core.geojson.SweetStyle;
import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public abstract class AbstractPropertyValueFilter implements Filter {
    abstract String property();
    abstract Object value();
    abstract PropertyValueOperator operator();

    @Override
    public boolean apply(Feature input) {
        Object featureValue = input.metadata().get(property());

        // No explicit null handling for now.
        switch (operator()) {
            case EQUAL:
                return featureValue.equals(value());
            case NOT_EQUAL:
                return ! featureValue.equals(value());
            default:
                throw new IllegalArgumentException("operator not recognised: " + operator());
        }
    }
}
