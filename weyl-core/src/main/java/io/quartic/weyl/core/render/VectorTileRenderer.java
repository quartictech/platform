package io.quartic.weyl.core.render;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.model.IndexedLayer;
import io.quartic.weyl.core.model.LayerId;
import no.ecc.vectortile.VectorTileEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class VectorTileRenderer {
    private static final Logger log = LoggerFactory.getLogger(VectorTileRenderer.class);
    private final Collection<IndexedLayer> layers;

    private static class VectorTileFeature {
        private final Map<String, Object> attributes;
        private final Geometry geometry;

        public static VectorTileFeature of(Geometry geometry, Map<String, Object> attributes) {
            return new VectorTileFeature(geometry, attributes);
        }

        private VectorTileFeature(Geometry geometry, Map<String, Object> attributes) {
            this.attributes = attributes;
            this.geometry = geometry;
        }

        public Geometry getGeometry() {
            return geometry;
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }
    }

    public VectorTileRenderer(Collection<IndexedLayer> layers) {
        this.layers = layers;
    }

    public byte[] render(int z, int x, int y) {
        Envelope bounds = Mercator.bounds(z, x, y);
        Coordinate southWest = Mercator.xy(new Coordinate(bounds.getMinX(), bounds.getMinY()));
        Coordinate northEast = Mercator.xy(new Coordinate(bounds.getMaxX(), bounds.getMaxY()));

        Envelope envelope = new Envelope(southWest, northEast);
        log.info("Envelope: {}", envelope.toString());

        VectorTileEncoder encoder = new VectorTileEncoder(4096, 8, false);
        for (IndexedLayer layer : layers) {
            final LayerId layerId = layer.layerId();
            log.info("encoding layer {}", layerId);
            final AtomicInteger featureCount = new AtomicInteger();

            Stopwatch stopwatch = Stopwatch.createStarted();
            layer.intersects(envelope).parallel().map( (feature) -> {
                Map<String, Object> attributes = Maps.newHashMapWithExpectedSize(feature.feature().metadata().size());

                for (Map.Entry<String, Optional<Object>> entry : feature.feature().metadata().entrySet()) {
                    attributes.put(entry.getKey(), entry.getValue().orElse(null));
                }

                attributes.put("_id", feature.feature().uid().uid());

                return VectorTileFeature.of(scaleGeometry(feature.feature().geometry(), envelope), attributes);
            }).sequential().forEach(vectorTileFeature -> {
                    featureCount.incrementAndGet();
                    encoder.addFeature(layerId.uid(),
                            vectorTileFeature.getAttributes(), vectorTileFeature.getGeometry());
            });
            log.info("encoded {} features in {}ms", featureCount.get(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }

        return encoder.encode();
    }

    private static Geometry scaleGeometry(Geometry geometry, Envelope envelope) {
        Geometry transformed = (Geometry) geometry.clone();
        transformed.apply(new CoordinateFilter() {
            @Override
            public void filter(Coordinate coord) {
                coord.x = 4096.0 * (coord.x - envelope.getMinX()) / (envelope.getWidth());
                coord.y = 4096.0 * (1 - (coord.y - envelope.getMinY()) / (envelope.getHeight()));
            }
        });
        transformed.geometryChanged();
        return transformed;
    }
}
