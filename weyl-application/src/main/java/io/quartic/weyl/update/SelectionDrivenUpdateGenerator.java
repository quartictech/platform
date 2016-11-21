package io.quartic.weyl.update;

import io.quartic.weyl.core.model.Feature;

import java.util.Collection;

public interface SelectionDrivenUpdateGenerator {
    String name();
    Object generate(Collection<Feature> entities);
}
