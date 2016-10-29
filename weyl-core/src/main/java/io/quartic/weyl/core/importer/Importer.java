package io.quartic.weyl.core.importer;

import io.quartic.weyl.core.model.Feature;

import java.io.IOException;
import java.util.Collection;

public interface Importer {
    Collection<Feature> get() throws IOException;
    void subscribe(ImporterSubscriber subscriber);
}
