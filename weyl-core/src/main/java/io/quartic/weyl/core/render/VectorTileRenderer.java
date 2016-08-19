package io.quartic.weyl.core.render;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.model.IndexedLayer;
import no.ecc.vectortile.VectorTileEncoder;

import java.util.Collection;

public class VectorTileRenderer {
    private final Collection<IndexedLayer> layers;

    public VectorTileRenderer(Collection<IndexedLayer> layers) {
        this.layers = layers;
    }

    public byte[] render(int z, int x, int y) {
        Envelope bounds = Mercator.bounds(z, x, y);
        Coordinate southWest = Mercator.xy(new Coordinate(bounds.getMinX(), bounds.getMinY()));
        Coordinate northEast = Mercator.xy(new Coordinate(bounds.getMaxX(), bounds.getMaxY()));

        Envelope envelope = new Envelope(southWest, northEast);

        VectorTileEncoder encoder = new VectorTileEncoder(4096, 8, false);
        for (IndexedLayer layer : layers) {
            layer.intersects(envelope).forEach(feature ->
                    encoder.addFeature(layer.layer().name(), feature.feature().metadata(),
                            scaleGeometry(feature.feature().geometry(), envelope))
            );
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
