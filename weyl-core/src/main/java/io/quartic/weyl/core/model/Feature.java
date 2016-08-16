package io.quartic.weyl.core.model;

import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
public interface Feature {
    String id();

    PreparedGeometry geometry();

    Map<String, Object> metadata();
}
