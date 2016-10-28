package io.quartic.weyl.common.uid;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

// TODO: make an iterator?
public class SequenceUidGenerator<T extends Uid> implements UidGenerator<T> {
    private final Function<String, T> converter;
    private final AtomicInteger counter = new AtomicInteger();

    public static <T extends Uid> SequenceUidGenerator<T> of(Function<String, T> converter) {
        return new SequenceUidGenerator<>(converter);
    }

    public SequenceUidGenerator(Function<String, T> converter) {
        this.converter = converter;
    }

    @Override
    public T get() {
        return converter.apply(String.valueOf(counter.incrementAndGet()));
    }
}
