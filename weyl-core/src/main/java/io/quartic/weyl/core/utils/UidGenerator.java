package io.quartic.weyl.core.utils;

import io.quartic.weyl.core.model.Uid;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

// TODO: make an iterator?
public class UidGenerator<T extends Uid> implements Supplier<T> {
    private final Function<String, T> converter;
    private final AtomicInteger counter = new AtomicInteger();

    public UidGenerator(Function<String, T> converter) {
        this.converter = converter;
    }

    public T get() {
        return converter.apply(String.valueOf(counter.incrementAndGet()));
    }
}
