package io.quartic.weyl.common.uid;

import java.util.function.Supplier;

public interface UidGenerator<T extends Uid> extends Supplier<T> {
    T get();
}
