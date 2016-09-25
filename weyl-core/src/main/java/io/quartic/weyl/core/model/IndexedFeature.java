package io.quartic.weyl.core.model;

import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import org.immutables.value.Value;

@Value.Immutable
public interface IndexedFeature {
    PreparedGeometry preparedGeometry();

    // underlying feature
    Feature feature();
}
