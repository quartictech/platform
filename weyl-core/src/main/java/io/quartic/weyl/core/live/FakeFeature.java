package io.quartic.weyl.core.live;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.quartic.weyl.core.geojson.Geometry;
import io.quartic.weyl.core.geojson.Point;
import io.quartic.weyl.core.model.Feature;

import java.util.Map;
import java.util.Optional;

class FakeFeature implements Feature<Geometry> {
    public static final double CENTRE_LNG = -0.10;
    public static final double CENTRE_LAT = 51.4800;
    public static final double RADIUS = 0.1;

    private final int magic;
    private String id;
    private final Map<String, Optional<Object>> metadata;

    FakeFeature(String id, int magic, Map<String, Optional<Object>> metadata) {
        this.id = id;
        this.magic = magic;
        this.metadata = ImmutableMap.copyOf(metadata);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Geometry geometry() {
        long time = System.currentTimeMillis();

        final double radius = RADIUS / magic;
        final double lngDelta = CENTRE_LNG + radius * Math.cos(2 * Math.PI * time / (10_000 * magic));
        final double latDelta = radius * Math.sin(2 * Math.PI * time / (10_000 * magic));

        return Point.of(ImmutableList.of(CENTRE_LNG + lngDelta, CENTRE_LAT + ((magic % 2 == 0) ? latDelta : -latDelta)));
    }

    @Override
    public Map<String, Optional<Object>> metadata() {
        return metadata;
    }
}
