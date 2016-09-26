package io.quartic.weyl.core.utils;

import java.util.UUID;
import java.util.function.Function;

public final class Utils {
    private Utils() {}

    public static <T> T uuid(Function<String, T> converter) {
        return converter.apply(UUID.randomUUID().toString());
    }
}
