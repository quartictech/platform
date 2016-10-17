package io.quartic.weyl.core.v2;

import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.model.Feature;

import java.util.Collection;

public interface Layer {
    AbstractDefaultLayer withFeatures(Collection<Feature> features);
    Iterable<Feature> query(Geometry geometry);
}
