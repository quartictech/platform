package io.quartic.weyl.core.model;

import io.quartic.weyl.core.feature.FeatureCollection;
import io.quartic.weyl.core.geojson.SweetStyle;
import org.immutables.value.Value;


@SweetStyle
@Value.Immutable
public interface AbstractLayer {
    AttributeSchema schema();
    LayerMetadata metadata();
    FeatureCollection features();
}
