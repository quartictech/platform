package io.quartic.weyl.core.attributes;

import java.util.Map;

public final class AttributeUtils {
    private AttributeUtils() {}

    // TODO: this is gross and we need to find a better way to deal with this
    public static boolean isSimple(Object value) {
        return (value != null) && !(value instanceof ComplexAttribute) && !(value instanceof Map);
    }
}
