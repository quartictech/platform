package io.quartic.weyl.core.utils;

import io.quartic.weyl.core.model.Uid;

import java.util.UUID;
import java.util.function.Function;

public class RandomUidGenerator<T extends Uid> implements UidGenerator<T> {
    private final Function<String, T> converter;

    public RandomUidGenerator(Function<String, T> converter) {
        this.converter = converter;
    }

    @Override
    public T get() {
        return converter.apply(UUID.randomUUID().toString().substring(0, 6));
    }
}
