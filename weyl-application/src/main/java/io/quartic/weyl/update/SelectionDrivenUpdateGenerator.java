package io.quartic.weyl.update;

import io.quartic.weyl.core.model.AbstractFeature;

import java.util.Collection;

public interface SelectionDrivenUpdateGenerator {
    String name();
    Object generate(Collection<AbstractFeature> entities);
}
