package io.quartic.weyl.core.model;

import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

@Value.Immutable
public interface Feature<T> {
    String id();

    T geometry();

    Map<String, Optional<Object>> metadata();
}
