package io.quartic.weyl.core.render;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.model.AbstractLayer;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.LayerId;
import no.ecc.vectortile.VectorTileEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class VectorTileRenderer {
    private static final Logger log = LoggerFactory.getLogger(VectorTileRenderer.class);
    private final Collection<AbstractLayer> layers;

    private static class VectorTileFeature {
        private final Map<AttributeName, Object> attributes;
        private final Geometry geometry;

        public static VectorTileFeature of(Geometry geometry, Map<AttributeName, Object> attributes) {
            return new VectorTileFeature(geometry, attributes);
        }

        private VectorTileFeature(Geometry geometry, Map<AttributeName, Object> attributes) {
            this.attributes = attributes;
            this.geometry = geometry;
        }

        public Geometry getGeometry() {
            return geometry;
        }

        public Map<AttributeName, Object> getAttributes() {
            return attributes;
        }
    }

    public VectorTileRenderer(Collection<AbstractLayer> layers) {
        this.layers = layers;
    }

    public byte[] render(int z, int x, int y) {
        Envelope bounds = Mercator.bounds(z, x, y);
        Coordinate southWest = Mercator.xy(new Coordinate(bounds.getMinX(), bounds.getMinY()));
        Coordinate northEast = Mercator.xy(new Coordinate(bounds.getMaxX(), bounds.getMaxY()));

        Envelope envelope = new Envelope(southWest, northEast);
        log.info("Envelope: {}", envelope.toString());

        VectorTileEncoder encoder = new VectorTileEncoder(4096, 8, false);
        for (AbstractLayer layer : layers) {
            final LayerId layerId = layer.layerId();
            log.info("encoding layer {}", layerId);
            final AtomicInteger featureCount = new AtomicInteger();

            Stopwatch stopwatch = Stopwatch.createStarted();
            layer.intersects(envelope).parallel().map( (feature) -> {
                Map<AttributeName, Object> attributes = Maps.newHashMapWithExpectedSize(feature.feature().attributes().size());

                feature.feature().attributes().entrySet().stream()
                        .filter(entry -> !(entry.getValue() instanceof ComplexAttribute))
                        .forEach(entry -> attributes.put(entry.getKey(), entry.getValue()));

                attributes.put(AttributeName.of("_id"), feature.feature().uid().uid());

                return VectorTileFeature.of(scaleGeometry(feature.feature().geometry(), envelope), attributes);
            }).sequential().forEach(vectorTileFeature -> {
                    featureCount.incrementAndGet();
                    encoder.addFeature(layerId.uid(),
                            convert(vectorTileFeature.getAttributes()), vectorTileFeature.getGeometry());
            });
            log.info("encoded {} features in {}ms", featureCount.get(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }

        return encoder.encode();
    }

    private Map<String, Object> convert(Map<AttributeName, Object> attributes) {
        return attributes.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));
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
