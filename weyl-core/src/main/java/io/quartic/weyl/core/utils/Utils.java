package io.quartic.weyl.core.utils;

import java.util.UUID;
import java.util.function.Supplier;

public final class Utils {
    private Utils() {}

    public static Supplier<String> idSupplier() {
        return () -> UUID.randomUUID().toString();
    }
}
