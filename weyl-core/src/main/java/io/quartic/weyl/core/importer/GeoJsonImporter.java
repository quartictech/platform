package io.quartic.weyl.core.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.model.ImmutableFeature;
import io.quartic.weyl.core.utils.GeometryTransformer;
import org.geotools.geometry.jts.JTS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GeoJsonImporter implements Importer {
    private final FeatureCollection featureCollection;
    private final FeatureStore featureStore;
    private final GeometryTransformer geometryTransformer;

    public static GeoJsonImporter fromInputStream(InputStream inputStream, FeatureStore featureStore, ObjectMapper objectMapper) throws IOException, FactoryException {
        FeatureCollection featureCollection = objectMapper.readValue(inputStream, FeatureCollection.class);
        return new GeoJsonImporter(featureCollection, featureStore, GeometryTransformer.wgs84toWebMercator());
    }

    public static GeoJsonImporter fromObject(Object value, FeatureStore featureStore, ObjectMapper objectMapper) throws IOException {
        FeatureCollection featureCollection = objectMapper.convertValue(value, FeatureCollection.class);
        return new GeoJsonImporter(featureCollection, featureStore, GeometryTransformer.wgs84toWebMercator());
    }

    private GeoJsonImporter(FeatureCollection featureCollection, FeatureStore featureStore,
                            GeometryTransformer geometryTransformer) {
        this.featureCollection = featureCollection;
        this.featureStore = featureStore;
        this.geometryTransformer = geometryTransformer;
    }

    private Optional<io.quartic.weyl.core.model.Feature> toJts(Feature f) {
        // TODO: We are ignoring null geometries here (as well as in the live pipeline). We should figure out something better.
        return f.geometry().map(rawGeometry -> {
            Geometry transformedGeometry = geometryTransformer.transform(Utils.toJts(rawGeometry));
            return ImmutableFeature.builder()
                    .externalId(f.id().orElse(null))
                    .uid(featureStore.getFeatureIdGenerator().get())
                    .geometry(transformedGeometry)
                    .metadata(f.properties())
                    .build();
        });
    }

    @Override
    public Collection<io.quartic.weyl.core.model.Feature> get() {
        return featureCollection.features().stream().map(this::toJts)
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))
                .collect(Collectors.toList());
    }
}
