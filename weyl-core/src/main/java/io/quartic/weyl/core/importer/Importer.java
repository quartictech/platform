package io.quartic.weyl.core.importer;

import io.quartic.weyl.core.model.Feature;

import java.util.Collection;
import java.util.function.Supplier;

public interface Importer {
    Collection<Feature> get();
}
