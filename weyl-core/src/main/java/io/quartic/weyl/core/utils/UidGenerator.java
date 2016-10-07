package io.quartic.weyl.core.utils;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;

// TODO: make an iterator?
public class UidGenerator<T> implements Supplier<T> {
    private final Function<Long, T> converter;
    private final AtomicLong counter = new AtomicLong();

    public UidGenerator(Function<Long, T> converter) {
        this.converter = converter;
    }

    public T get() {
        return converter.apply(counter.incrementAndGet());
    }
}
