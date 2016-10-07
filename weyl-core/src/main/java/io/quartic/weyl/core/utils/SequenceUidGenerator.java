package io.quartic.weyl.core.utils;

import io.quartic.weyl.core.model.Uid;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

// TODO: make an iterator?
public class SequenceUidGenerator<T extends Uid> implements UidGenerator<T> {
    private final Function<String, T> converter;
    private final AtomicInteger counter = new AtomicInteger();

    public SequenceUidGenerator(Function<String, T> converter) {
        this.converter = converter;
    }

    @Override
    public T get() {
        return converter.apply(String.valueOf(counter.incrementAndGet()));
    }
}
