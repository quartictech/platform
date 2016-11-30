package io.quartic.weyl.core.compute;

public class BucketUtils {
    static Double mapToDouble(Object value) {
        if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        else if (value instanceof Double || value instanceof Float) {
            return (double) value;
        }
        else if (value instanceof Long) {
            return ((Long) value).doubleValue();
        }
        else return Double.valueOf(value.toString());
    }
}
