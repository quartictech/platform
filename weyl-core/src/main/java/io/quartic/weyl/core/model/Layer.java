package io.quartic.weyl.core.model;

import org.immutables.value.Value;

import java.util.Collection;

public interface Layer {
    String name();
    Collection<Feature> features();
}
