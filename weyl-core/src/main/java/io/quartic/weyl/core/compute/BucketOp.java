package io.quartic.weyl.core.compute;

import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.Layer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class BucketOp {
    private final Layer points;
    private final Layer polygons;

    public BucketOp(Layer points, Layer polygons) {
        this.points = points;
        this.polygons = polygons;
    }

    public Layer compute() {
        Map<String, >
        this.points.features().parallelStream()
                .forEach(feature -> {
                    Geometry point = feature.geometry().getGeometry();

                    List<Feature> hits = polygons.index().query(point.getEnvelopeInternal());

                    hits.stream().filter(feature -> feature.geometry().getGeometry().contains(point))
                            .forEach();
                });
    }

}
