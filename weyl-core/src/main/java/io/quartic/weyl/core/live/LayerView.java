package io.quartic.weyl.core.live;

import io.quartic.weyl.core.model.AbstractFeature;

import java.util.Collection;
import java.util.stream.Stream;

public interface LayerView {
    LayerView IDENTITY_VIEW = Collection::stream;

    // TODO: Figure out time-ordering here
    Stream<AbstractFeature> compute(Collection<AbstractFeature> history);
}
