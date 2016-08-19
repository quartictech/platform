package io.quartic.weyl.core.model;

import java.util.Collection;

public interface Layer {
    String name();
    Collection<Feature> features();
}
