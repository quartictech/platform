package io.quartic.weyl.core.model;

import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

@Value.Immutable
public interface Feature<T> {
    @Value.Parameter String id();

    @Value.Parameter T geometry();

    @Value.Parameter Map<String, Optional<Object>> metadata();
}
