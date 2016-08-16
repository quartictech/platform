package io.quartic.weyl.core.model;

import com.vividsolutions.jts.index.SpatialIndex;
import org.immutables.value.Value;

import java.util.Collection;

@Value.Immutable
public interface Layer {
    String name();
    Collection<Feature> features();
    SpatialIndex index();
}
