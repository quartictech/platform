package io.quartic.weyl.core.compute.bucket;

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
        else return null;
    }
}
