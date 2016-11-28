package io.quartic.weyl.core.model;

import com.vividsolutions.jts.geom.Geometry;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.Optional;

@SweetStyle
@Value.Immutable
public interface NakedFeature {
    Optional<String> externalId();
    Geometry geometry();
    Attributes attributes();
}
