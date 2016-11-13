package io.quartic.weyl.core.live;

import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.common.uid.UidGenerator;

import java.util.Collection;
import java.util.stream.Stream;

public interface LayerView {
    LayerView IDENTITY_VIEW = (g, f) -> f.stream();

    // TODO: Figure out time-ordering here
    Stream<Feature> compute(UidGenerator<FeatureId> uidGenerator, Collection<Feature> history);
}
