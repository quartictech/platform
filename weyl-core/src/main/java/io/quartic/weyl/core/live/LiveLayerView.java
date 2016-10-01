package io.quartic.weyl.core.live;

import io.quartic.weyl.core.model.Feature;

import java.util.Collection;
import java.util.stream.Stream;

public interface LiveLayerView {
    // TODO: Figure out time-ordering here
    Stream<Feature> compute(Collection<Feature> history);
}
