package io.quartic.weyl.core.utils;

import io.quartic.weyl.core.model.Uid;

import java.util.function.Supplier;

public interface UidGenerator<T extends Uid> extends Supplier<T> {
    T get();
}
