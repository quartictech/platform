package io.quartic.weyl.core.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.model.ImmutableFeature;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
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
    private final MathTransform mathTransform;

    public static GeoJsonImporter fromInputStream(InputStream inputStream, FeatureStore featureStore, ObjectMapper objectMapper) throws IOException, FactoryException {
        FeatureCollection featureCollection = objectMapper.readValue(inputStream, FeatureCollection.class);
        return new GeoJsonImporter(featureCollection, featureStore, findMathTransform());
    }

    static MathTransform findMathTransform() throws FactoryException {
        // Ugh. See http://docs.geotools.org/latest/userguide/library/referencing/order.html
        CRSAuthorityFactory factory = CRS.getAuthorityFactory(true);
        CoordinateReferenceSystem sourceCrs = factory.createCoordinateReferenceSystem("EPSG:4326");
        CoordinateReferenceSystem targetCrs = CRS.decode("EPSG:3857");
        return CRS.findMathTransform(sourceCrs, targetCrs);
    }

    public static GeoJsonImporter fromObject(Object value, FeatureStore featureStore, ObjectMapper objectMapper) throws IOException {
        FeatureCollection featureCollection = objectMapper.convertValue(value, FeatureCollection.class);
        try {
            return new GeoJsonImporter(featureCollection, featureStore, findMathTransform());
        } catch (FactoryException e) {
            throw new IOException(e);
        }
    }

    private GeoJsonImporter(FeatureCollection featureCollection, FeatureStore featureStore, MathTransform mathTransform) {
        this.featureCollection = featureCollection;
        this.featureStore = featureStore;
        this.mathTransform = mathTransform;
    }

    private Optional<io.quartic.weyl.core.model.Feature> toJts(Feature f) {
        Geometry transformed;
        try {
            transformed = JTS.transform(Utils.toJts(f.geometry()), mathTransform);
        } catch (TransformException e) {
            e.printStackTrace();
            return Optional.empty();
        }
        return Optional.of(ImmutableFeature.builder()
                .externalId(f.id().get())
                .uid(featureStore.getFeatureIdGenerator().get())
                .geometry(transformed)
                .metadata(f.properties())
                .build());
    }

    @Override
    public Collection<io.quartic.weyl.core.model.Feature> get() {
        return featureCollection.features().stream().map(this::toJts)
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))
                .collect(Collectors.toList());
    }
}
